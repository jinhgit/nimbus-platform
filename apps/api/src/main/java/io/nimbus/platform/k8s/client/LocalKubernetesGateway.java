package io.nimbus.platform.k8s.client;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.k8s.config.KubernetesProperties;
import io.nimbus.platform.k8s.domain.DeployStatus;
import io.nimbus.platform.k8s.dto.K8sDtos;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * free-only 로컬 클러스터 게이트웨이 (k3d / kind / minikube / docker-desktop).
 * 원격 EKS 등 과금 컨텍스트는 strictLocal 로 차단.
 */
@Component
public class LocalKubernetesGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalKubernetesGateway.class);

    private final KubernetesProperties properties;
    private volatile KubernetesClient client;
    private volatile String activeContext;
    private volatile String clusterType;
    private volatile String unavailableReason = "not initialized";

    public LocalKubernetesGateway(KubernetesProperties properties) {
        this.properties = properties;
        reconnect();
    }

    public synchronized void reconnect() {
        closeQuietly();
        if (!properties.isEnabled()) {
            unavailableReason = "nimbus.k8s.enabled=false";
            return;
        }
        try {
            Config config = buildConfig();
            String context = config.getCurrentContext() != null
                    ? config.getCurrentContext().getName()
                    : config.getNamespace(); // fallback
            // fabric8: getCurrentContext may be Context object
            String contextName = resolveContextName(config);
            if (properties.isStrictLocal() && !isAllowedContext(contextName)) {
                unavailableReason = "context denied for free-only: " + contextName
                        + " (allowed: k3d-*, kind-*, minikube, docker-desktop)";
                log.warn("Kubernetes context blocked: {}", unavailableReason);
                return;
            }
            this.client = new KubernetesClientBuilder().withConfig(config).build();
            // probe
            this.client.getKubernetesVersion();
            this.activeContext = contextName;
            this.clusterType = detectClusterType(contextName);
            this.unavailableReason = null;
            log.info("Connected to local Kubernetes context={} type={}", activeContext, clusterType);
        } catch (Exception e) {
            this.client = null;
            this.activeContext = null;
            this.clusterType = null;
            this.unavailableReason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Local Kubernetes unavailable: {}", unavailableReason);
        }
    }

    public boolean isAvailable() {
        return client != null;
    }

    /** Nullable client for optional CRD probes (Argo etc.). */
    public KubernetesClient clientOrNull() {
        return client;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public String getActiveContext() {
        return activeContext;
    }

    public String getClusterType() {
        return clusterType;
    }

    public K8sDtos.ClusterStatusResponse status() {
        if (!properties.isEnabled()) {
            return new K8sDtos.ClusterStatusResponse(
                    false, false, null, null, null, "disabled", 0, 0
            );
        }
        if (client == null) {
            return new K8sDtos.ClusterStatusResponse(
                    false, true, null, null, null, unavailableReason, 0, 0
            );
        }
        try {
            String version = client.getKubernetesVersion().getGitVersion();
            int nodes = client.nodes().list().getItems().size();
            int namespaces = client.namespaces().list().getItems().size();
            return new K8sDtos.ClusterStatusResponse(
                    true, true, activeContext, clusterType, version,
                    "connected", nodes, namespaces
            );
        } catch (Exception e) {
            return new K8sDtos.ClusterStatusResponse(
                    false, true, activeContext, clusterType, null, e.getMessage(), 0, 0
            );
        }
    }

    public K8sDtos.DeployResult deploy(
            String serviceName,
            String environment,
            int replicas,
            String image
    ) {
        if (client == null) {
            throw new BusinessException(ErrorCode.K8S_UNAVAILABLE, unavailableReason);
        }
        String ns = sanitizeK8sName(serviceName + "-" + (environment != null ? environment : "dev").toLowerCase(Locale.ROOT));
        String deployName = sanitizeK8sName(serviceName);
        String img = image != null && !image.isBlank() ? image : properties.getDemoImage();
        int rep = Math.max(1, replicas);

        try {
            ensureNamespace(ns);
            applyDeployment(ns, deployName, img, rep);
            applyService(ns, deployName);

            int ready = waitForReady(ns, deployName, rep, properties.getDeployTimeoutSeconds());
            DeployStatus st = ready >= rep ? DeployStatus.RUNNING
                    : ready > 0 ? DeployStatus.DEGRADED : DeployStatus.FAILED;

            return new K8sDtos.DeployResult(
                    true, ns, deployName, img, rep, ready,
                    activeContext, clusterType, st,
                    st == DeployStatus.RUNNING ? "Deployment ready" : "Deployment not fully ready (" + ready + "/" + rep + ")"
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("K8s deploy failed", e);
            throw new BusinessException(ErrorCode.K8S_DEPLOY_FAILED, e.getMessage());
        }
    }

    public List<K8sDtos.PodSummary> listPods(String namespace, String appLabel) {
        if (client == null) {
            return List.of();
        }
        try {
            List<Pod> pods = client.pods().inNamespace(namespace)
                    .withLabel("app", appLabel)
                    .list()
                    .getItems();
            List<K8sDtos.PodSummary> result = new ArrayList<>();
            for (Pod pod : pods) {
                String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";
                int restarts = 0;
                boolean ready = false;
                if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
                    restarts = pod.getStatus().getContainerStatuses().stream()
                            .mapToInt(cs -> cs.getRestartCount() != null ? cs.getRestartCount() : 0)
                            .sum();
                    ready = pod.getStatus().getContainerStatuses().stream()
                            .allMatch(cs -> Boolean.TRUE.equals(cs.getReady()));
                }
                result.add(new K8sDtos.PodSummary(
                        pod.getMetadata().getName(),
                        phase,
                        pod.getSpec() != null ? pod.getSpec().getNodeName() : null,
                        restarts,
                        ready
                ));
            }
            return result;
        } catch (Exception e) {
            log.warn("listPods failed: {}", e.getMessage());
            return List.of();
        }
    }

    public int countReadyReplicas(String namespace, String deploymentName) {
        if (client == null) {
            return 0;
        }
        try {
            Deployment d = client.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
            if (d == null || d.getStatus() == null || d.getStatus().getReadyReplicas() == null) {
                return 0;
            }
            return d.getStatus().getReadyReplicas();
        } catch (Exception e) {
            return 0;
        }
    }

    private void ensureNamespace(String ns) {
        Namespace existing = client.namespaces().withName(ns).get();
        if (existing != null) {
            return;
        }
        client.namespaces().resource(new NamespaceBuilder()
                .withNewMetadata()
                .withName(ns)
                .addToLabels("app.kubernetes.io/managed-by", "nimbus")
                .addToLabels("nimbus.platform/env", "local")
                .endMetadata()
                .build()).create();
    }

    private void applyDeployment(String ns, String name, String image, int replicas) {
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(ns)
                .addToLabels("app", name)
                .addToLabels("app.kubernetes.io/managed-by", "nimbus")
                .endMetadata()
                .withNewSpec()
                .withReplicas(replicas)
                .withNewSelector()
                .addToMatchLabels("app", name)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("app")
                .withImage(image)
                .addNewPort().withContainerPort(80).endPort()
                .withNewReadinessProbe()
                .withNewHttpGet()
                .withPath("/")
                .withNewPort(80)
                .endHttpGet()
                .withInitialDelaySeconds(3)
                .withPeriodSeconds(5)
                .endReadinessProbe()
                .withNewLivenessProbe()
                .withNewHttpGet()
                .withPath("/")
                .withNewPort(80)
                .endHttpGet()
                .withInitialDelaySeconds(10)
                .withPeriodSeconds(10)
                .endLivenessProbe()
                .withNewResources()
                .addToRequests(Map.of(
                        "cpu", new io.fabric8.kubernetes.api.model.Quantity("50m"),
                        "memory", new io.fabric8.kubernetes.api.model.Quantity("64Mi")
                ))
                .addToLimits(Map.of(
                        "cpu", new io.fabric8.kubernetes.api.model.Quantity("200m"),
                        "memory", new io.fabric8.kubernetes.api.model.Quantity("128Mi")
                ))
                .endResources()
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        var deployOps = client.apps().deployments().inNamespace(ns).resource(deployment);
        if (client.apps().deployments().inNamespace(ns).withName(name).get() == null) {
            deployOps.create();
        } else {
            deployOps.update();
        }
    }

    private void applyService(String ns, String name) {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(ns)
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                .addToSelector("app", name)
                .addNewPort()
                .withName("http")
                .withPort(80)
                .withNewTargetPort(80)
                .endPort()
                .withType("ClusterIP")
                .endSpec()
                .build();
        if (client.services().inNamespace(ns).withName(name).get() == null) {
            client.services().inNamespace(ns).resource(service).create();
        } else {
            client.services().inNamespace(ns).resource(service).update();
        }
    }

    private int waitForReady(String ns, String name, int desired, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        int ready = 0;
        while (System.currentTimeMillis() < deadline) {
            ready = countReadyReplicas(ns, name);
            if (ready >= desired) {
                return ready;
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return ready;
    }

    private Config buildConfig() throws Exception {
        String path = properties.getKubeconfig();
        if (path != null && !path.isBlank() && Files.exists(Path.of(path))) {
            String yaml = Files.readString(Path.of(path));
            return Config.fromKubeconfig(yaml);
        }
        return Config.autoConfigure(null);
    }

    private String resolveContextName(Config config) {
        // fabric8 Config#getCurrentContext() returns context name (String) in recent versions
        try {
            Object ctx = config.getCurrentContext();
            if (ctx instanceof String s && !s.isBlank()) {
                return s;
            }
            if (ctx != null) {
                try {
                    var m = ctx.getClass().getMethod("getName");
                    Object n = m.invoke(ctx);
                    if (n != null && !n.toString().isBlank()) {
                        return n.toString();
                    }
                } catch (Exception ignored) {
                    // fall through
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        // env fallback used by kubectl
        String envCtx = System.getenv("KUBECONFIG_CONTEXT");
        if (envCtx != null && !envCtx.isBlank()) {
            return envCtx;
        }
        String master = config.getMasterUrl();
        return master != null ? master : "unknown";
    }

    private boolean isAllowedContext(String contextName) {
        if (contextName == null || contextName.isBlank()) {
            return false;
        }
        // block obvious cloud contexts
        String lower = contextName.toLowerCase(Locale.ROOT);
        if (lower.contains("eks") || lower.contains("arn:aws") || lower.contains("gke_")
                || lower.contains("aks") || lower.contains("azure")) {
            return false;
        }
        for (String pattern : properties.getAllowedContextPatterns()) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(contextName).matches()) {
                return true;
            }
        }
        return false;
    }

    private static String detectClusterType(String contextName) {
        if (contextName == null) return "unknown";
        String c = contextName.toLowerCase(Locale.ROOT);
        if (c.startsWith("k3d-") || c.contains("k3d")) return "k3d";
        if (c.startsWith("kind-") || c.contains("kind")) return "kind";
        if (c.contains("minikube")) return "minikube";
        if (c.contains("docker-desktop")) return "docker-desktop";
        if (c.contains("orbstack")) return "orbstack";
        return "local";
    }

    public static String sanitizeK8sName(String raw) {
        String s = raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("(^-|-$)", "");
        if (s.isBlank()) {
            s = "nimbus-app";
        }
        if (s.length() > 63) {
            s = s.substring(0, 63).replaceAll("-$", "");
        }
        return s;
    }

    @PreDestroy
    public void destroy() {
        closeQuietly();
    }

    private void closeQuietly() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
                // ignore
            }
            client = null;
        }
    }
}
