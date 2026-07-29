package io.nimbus.platform.observability.service;

import io.nimbus.platform.k8s.domain.DeployStatus;
import io.nimbus.platform.k8s.domain.K8sDeploymentRecord;
import io.nimbus.platform.k8s.repository.K8sDeploymentRecordRepository;
import io.nimbus.platform.observability.config.ObservabilityProperties;
import io.nimbus.platform.observability.dto.ObservabilityDtos;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final ObservabilityProperties properties;
    private final AppServiceRepository appServiceRepository;
    private final K8sDeploymentRecordRepository deploymentRepository;
    private final RestClient restClient;

    public MonitoringService(
            ObservabilityProperties properties,
            AppServiceRepository appServiceRepository,
            K8sDeploymentRecordRepository deploymentRepository
    ) {
        this.properties = properties;
        this.appServiceRepository = appServiceRepository;
        this.deploymentRepository = deploymentRepository;
        this.restClient = RestClient.builder().build();
    }

    public ObservabilityDtos.StackLinks links() {
        boolean prom = ping(properties.getPrometheusUrl() + "/-/ready");
        boolean graf = ping(properties.getGrafanaUrl() + "/api/health");
        String mode = prom || graf ? "live" : "demo";
        return new ObservabilityDtos.StackLinks(
                properties.getPrometheusUrl(),
                properties.getGrafanaUrl(),
                properties.getLokiUrl(),
                prom,
                graf,
                mode
        );
    }

    @Transactional(readOnly = true)
    public ObservabilityDtos.MonitoringOverview overview(UUID workspaceId) {
        List<AppService> services = workspaceId != null
                ? appServiceRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(workspaceId)
                : List.of();
        int running;
        if (workspaceId != null) {
            running = (int) deploymentRepository
                    .findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId)
                    .stream()
                    .filter(d -> d.getStatus() == DeployStatus.RUNNING || d.getStatus() == DeployStatus.SIMULATED)
                    .count();
        } else {
            running = (int) deploymentRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                    .filter(d -> d.getStatus() == DeployStatus.RUNNING || d.getStatus() == DeployStatus.SIMULATED)
                    .count();
        }

        List<ObservabilityDtos.ServiceMetrics> top = new ArrayList<>();
        double cpuSum = 0;
        double memSum = 0;
        int n = 0;
        for (AppService s : services.stream().limit(8).toList()) {
            ObservabilityDtos.ServiceMetrics m = metricsForService(s);
            top.add(m);
            for (var p : m.metrics()) {
                if ("cpu".equals(p.name())) {
                    cpuSum += p.value();
                    n++;
                }
                if ("memory".equals(p.name())) {
                    memSum += p.value();
                }
            }
        }
        ObservabilityDtos.StackLinks stack = links();
        return new ObservabilityDtos.MonitoringOverview(
                stack,
                services.size(),
                running,
                n == 0 ? 0 : round(cpuSum / n),
                n == 0 ? 0 : round(memSum / Math.max(n, 1)),
                top
        );
    }

    @Transactional(readOnly = true)
    public ObservabilityDtos.ServiceMetrics serviceMetrics(UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new io.nimbus.platform.common.exception.BusinessException(
                        io.nimbus.platform.common.exception.ErrorCode.SERVICE_NOT_FOUND));
        return metricsForService(service);
    }

    private ObservabilityDtos.ServiceMetrics metricsForService(AppService service) {
        // Try Prometheus query; fall back to demo metrics seeded by service state
        if (links().prometheusUp()) {
            try {
                Double cpu = queryPrometheus("process_cpu_usage");
                if (cpu != null) {
                    return new ObservabilityDtos.ServiceMetrics(
                            service.getId(),
                            service.getName(),
                            "prometheus",
                            List.of(
                                    new ObservabilityDtos.MetricPoint("cpu", round(cpu * 100), "%"),
                                    new ObservabilityDtos.MetricPoint("memory", 42.0, "%"),
                                    new ObservabilityDtos.MetricPoint("replicas",
                                            service.getReplicaCount() != null ? service.getReplicaCount() : 1, "count"),
                                    new ObservabilityDtos.MetricPoint("restarts", 0, "count")
                            ),
                            Instant.now()
                    );
                }
            } catch (Exception e) {
                log.debug("Prometheus query failed: {}", e.getMessage());
            }
        }

        if (!properties.isDemoMetricsWhenUnavailable()) {
            return new ObservabilityDtos.ServiceMetrics(
                    service.getId(), service.getName(), "unavailable", List.of(), Instant.now()
            );
        }

        var rnd = ThreadLocalRandom.current();
        boolean healthy = service.getK8sStatus() == null
                || "RUNNING".equals(service.getK8sStatus())
                || "SIMULATED".equals(service.getK8sStatus())
                || "READY".equals(service.getStatus() != null ? service.getStatus().name() : "");
        double cpu = healthy ? 8 + rnd.nextDouble(0, 25) : 70 + rnd.nextDouble(0, 25);
        double mem = healthy ? 20 + rnd.nextDouble(0, 30) : 75 + rnd.nextDouble(0, 20);
        int restarts = healthy ? rnd.nextInt(0, 2) : rnd.nextInt(3, 8);
        int replicas = service.getReplicaCount() != null ? service.getReplicaCount() : 1;
        int ready = "RUNNING".equals(service.getK8sStatus()) ? replicas
                : "SIMULATED".equals(service.getK8sStatus()) ? replicas
                : Math.max(0, replicas - 1);

        return new ObservabilityDtos.ServiceMetrics(
                service.getId(),
                service.getName(),
                "demo",
                List.of(
                        new ObservabilityDtos.MetricPoint("cpu", round(cpu), "%"),
                        new ObservabilityDtos.MetricPoint("memory", round(mem), "%"),
                        new ObservabilityDtos.MetricPoint("replicas", replicas, "count"),
                        new ObservabilityDtos.MetricPoint("readyReplicas", ready, "count"),
                        new ObservabilityDtos.MetricPoint("restarts", restarts, "count"),
                        new ObservabilityDtos.MetricPoint("rps", round(10 + rnd.nextDouble(0, 90)), "req/s")
                ),
                Instant.now()
        );
    }

    private Double queryPrometheus(String query) {
        try {
            String url = properties.getPrometheusUrl() + "/api/v1/query?query=" + query;
            String body = restClient.get().uri(url).retrieve().body(String.class);
            if (body == null) return null;
            // minimal parse: "value":[ts,"0.12"]
            int idx = body.lastIndexOf("\"");
            if (idx <= 0) return null;
            int start = body.lastIndexOf("\"", idx - 1);
            if (start < 0) return null;
            String raw = body.substring(start + 1, idx);
            return Double.parseDouble(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean ping(String url) {
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
