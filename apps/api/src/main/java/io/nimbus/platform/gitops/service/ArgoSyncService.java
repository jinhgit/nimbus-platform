package io.nimbus.platform.gitops.service;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.gitops.dto.ArgoDtos;
import io.nimbus.platform.k8s.client.LocalKubernetesGateway;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Thin ArgoCD Application status.
 * LIVE when cluster has argoproj Application CR; otherwise SIMULATED with generated manifest.
 */
@Service
public class ArgoSyncService {

    private static final Logger log = LoggerFactory.getLogger(ArgoSyncService.class);

    private final AppServiceRepository appServiceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final LocalKubernetesGateway kubernetesGateway;

    public ArgoSyncService(
            AppServiceRepository appServiceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            LocalKubernetesGateway kubernetesGateway
    ) {
        this.appServiceRepository = appServiceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.kubernetesGateway = kubernetesGateway;
    }

    @Transactional(readOnly = true)
    public ArgoDtos.ArgoSyncResponse status(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());

        String appName = service.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        String ns = service.getK8sNamespace() != null ? service.getK8sNamespace() : appName + "-dev";
        String repo = service.getGithubRepoUrl() != null
                ? service.getGithubRepoUrl()
                : "https://github.com/example/" + appName + ".git";
        String manifest = buildManifest(appName, ns, repo, service);

        KubernetesClient client = kubernetesGateway.clientOrNull();
        if (client == null || !kubernetesGateway.isAvailable()) {
            return simulated(service, appName, ns, repo, manifest,
                    "클러스터 미연결 — Argo Application 매니페스트만 제공 (SIMULATED)");
        }

        try {
            // Generic CRD: argoproj.io/v1alpha1 Application in argocd ns (or any)
            var list = client.genericKubernetesResources("argoproj.io/v1alpha1", "Application")
                    .inAnyNamespace()
                    .list();
            if (list == null || list.getItems() == null || list.getItems().isEmpty()) {
                return simulated(service, appName, ns, repo, manifest,
                        "클러스터에 Argo Application CR 없음 — SIMULATED");
            }
            for (var item : list.getItems()) {
                String name = item.getMetadata() != null ? item.getMetadata().getName() : null;
                if (name == null) continue;
                if (!name.equals(appName) && !name.contains(appName)) continue;

                Map<String, Object> status = item.getAdditionalProperties() != null
                        ? castMap(item.getAdditionalProperties().get("status"))
                        : null;
                String sync = status != null ? stringAt(status, "sync", "status") : "Unknown";
                String health = status != null ? stringAt(status, "health", "status") : "Unknown";
                String rev = status != null ? stringAt(status, "sync", "revision") : null;
                return new ArgoDtos.ArgoSyncResponse(
                        service.getId(),
                        service.getName(),
                        "LIVE",
                        sync != null ? sync : "Unknown",
                        health != null ? health : "Unknown",
                        name,
                        item.getMetadata().getNamespace(),
                        repo,
                        rev != null ? rev : "HEAD",
                        "Argo Application CR 발견",
                        manifest
                );
            }
            return simulated(service, appName, ns, repo, manifest,
                    "매칭 Application 없음 (" + list.getItems().size() + " apps) — SIMULATED");
        } catch (Exception ex) {
            log.info("Argo CR probe failed: {}", ex.getMessage());
            return simulated(service, appName, ns, repo, manifest,
                    "Argo API 조회 실패 — SIMULATED: " + truncate(ex.getMessage()));
        }
    }

    private ArgoDtos.ArgoSyncResponse simulated(
            AppService service,
            String appName,
            String ns,
            String repo,
            String manifest,
            String message
    ) {
        return new ArgoDtos.ArgoSyncResponse(
                service.getId(),
                service.getName(),
                "SIMULATED",
                "Synced",
                "Healthy",
                appName,
                "argocd",
                repo,
                "main",
                message,
                manifest
        );
    }

    private static String buildManifest(String appName, String destNs, String repo, AppService service) {
        String env = service.getEnvironmentType() != null
                ? service.getEnvironmentType().name().toLowerCase(Locale.ROOT)
                : "dev";
        return """
                apiVersion: argoproj.io/v1alpha1
                kind: Application
                metadata:
                  name: %s
                  namespace: argocd
                  labels:
                    nimbus.io/service: %s
                    nimbus.io/environment: %s
                spec:
                  project: default
                  source:
                    repoURL: %s
                    targetRevision: HEAD
                    path: deploy/%s
                  destination:
                    server: https://kubernetes.default.svc
                    namespace: %s
                  syncPolicy:
                    automated:
                      prune: true
                      selfHeal: true
                    syncOptions:
                      - CreateNamespace=true
                """.formatted(appName, service.getName(), env, repo, env, destNs);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String stringAt(Map<String, Object> root, String k1, String k2) {
        Object nested = root.get(k1);
        if (nested instanceof Map<?, ?> m) {
            Object v = m.get(k2);
            return v != null ? String.valueOf(v) : null;
        }
        return null;
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 120 ? s.substring(0, 120) : s;
    }
}
