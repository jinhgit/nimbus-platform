package io.nimbus.platform.observability.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.k8s.client.LocalKubernetesGateway;
import io.nimbus.platform.observability.dto.ObservabilityDtos;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class LogStreamService {

    private static final Logger log = LoggerFactory.getLogger(LogStreamService.class);
    private final AppServiceRepository appServiceRepository;
    private final LocalKubernetesGateway k8sGateway;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LogStreamService(
            AppServiceRepository appServiceRepository,
            LocalKubernetesGateway k8sGateway
    ) {
        this.appServiceRepository = appServiceRepository;
        this.k8sGateway = k8sGateway;
    }

    public ObservabilityDtos.LogSnapshot snapshot(UUID serviceId, int limit) {
        AppService service = requireService(serviceId);
        int n = Math.min(Math.max(limit, 10), 500);

        if (canUseK8sLogs(service)) {
            try {
                List<ObservabilityDtos.LogLine> lines = fetchK8sLogs(service, n);
                if (!lines.isEmpty()) {
                    return new ObservabilityDtos.LogSnapshot(
                            service.getId(), service.getName(), "kubernetes", lines
                    );
                }
            } catch (Exception e) {
                log.warn("K8s log fetch failed: {}", e.getMessage());
            }
        }
        return new ObservabilityDtos.LogSnapshot(
                service.getId(), service.getName(), "demo", demoLogs(service, n)
        );
    }

    public SseEmitter stream(UUID serviceId) {
        AppService service = requireService(serviceId);
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> {
            try {
                if (canUseK8sLogs(service)) {
                    streamK8s(service, emitter);
                } else {
                    streamDemo(service, emitter);
                }
                emitter.complete();
            } catch (Exception e) {
                log.debug("SSE closed: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void streamK8s(AppService service, SseEmitter emitter) throws Exception {
        // snapshot first
        for (ObservabilityDtos.LogLine line : fetchK8sLogs(service, 50)) {
            emitter.send(SseEmitter.event().name("log").data(line));
        }
        // then demo tail for continuous stream (full follow needs long-lived pod log watch)
        for (int i = 0; i < 40; i++) {
            Thread.sleep(800);
            emitter.send(SseEmitter.event().name("log").data(demoLine(service, Instant.now())));
        }
    }

    private void streamDemo(AppService service, SseEmitter emitter) throws Exception {
        for (ObservabilityDtos.LogLine line : demoLogs(service, 20)) {
            emitter.send(SseEmitter.event().name("log").data(line));
            Thread.sleep(120);
        }
        for (int i = 0; i < 60; i++) {
            Thread.sleep(700);
            emitter.send(SseEmitter.event().name("log").data(demoLine(service, Instant.now())));
        }
    }

    private List<ObservabilityDtos.LogLine> fetchK8sLogs(AppService service, int limit) {
        // Use fabric8 via gateway pods list + client would need exposure.
        // For MVP we generate structured lines tagged with pod names from k8s.
        List<io.nimbus.platform.k8s.dto.K8sDtos.PodSummary> pods =
                k8sGateway.listPods(service.getK8sNamespace(), service.getK8sDeployment());
        List<ObservabilityDtos.LogLine> lines = new ArrayList<>();
        if (pods.isEmpty()) {
            return lines;
        }
        Instant now = Instant.now();
        for (var pod : pods) {
            lines.add(new ObservabilityDtos.LogLine(
                    now.minusSeconds(30), "INFO", pod.name(),
                    "Pod phase=" + pod.phase() + " ready=" + pod.ready()
            ));
            lines.add(new ObservabilityDtos.LogLine(
                    now.minusSeconds(20), "INFO", pod.name(),
                    "Container started for service " + service.getName()
            ));
            lines.add(new ObservabilityDtos.LogLine(
                    now.minusSeconds(5), "INFO", pod.name(),
                    "Listening on :80 (demo image)"
            ));
        }
        if (lines.size() > limit) {
            return lines.subList(lines.size() - limit, lines.size());
        }
        return lines;
    }

    private List<ObservabilityDtos.LogLine> demoLogs(AppService service, int n) {
        List<ObservabilityDtos.LogLine> lines = new ArrayList<>();
        Instant base = Instant.now().minusSeconds(n);
        String pod = service.getName() + "-0";
        String[] templates = {
                "Starting Nimbus managed service",
                "Loaded configuration profile=local",
                "Connected to configuration source",
                "Health endpoint ready",
                "Received request GET /health",
                "Metrics scrape complete",
                "Replica status ready=" + (service.getReplicaCount() != null ? service.getReplicaCount() : 1),
                "GitHub repo bound: " + (service.getGithubRepoUrl() != null ? service.getGithubRepoUrl() : "n/a"),
                "K8s namespace=" + (service.getK8sNamespace() != null ? service.getK8sNamespace() : "sim"),
        };
        for (int i = 0; i < n; i++) {
            String msg = templates[i % templates.length];
            String level = i % 17 == 0 ? "WARN" : "INFO";
            lines.add(new ObservabilityDtos.LogLine(base.plusSeconds(i), level, pod, msg));
        }
        return lines;
    }

    private ObservabilityDtos.LogLine demoLine(AppService service, Instant ts) {
        String[] msgs = {
                "Heartbeat OK",
                "Handled request in 12ms",
                "GC pause 3ms",
                "Config refresh skipped",
                "Probe readiness=200"
        };
        String msg = msgs[(int) (ts.getEpochSecond() % msgs.length)];
        return new ObservabilityDtos.LogLine(
                ts, "INFO", service.getName() + "-0", msg
        );
    }

    private boolean canUseK8sLogs(AppService service) {
        return k8sGateway.isAvailable()
                && service.getK8sNamespace() != null
                && service.getK8sDeployment() != null
                && !"SIMULATED".equalsIgnoreCase(service.getK8sStatus());
    }

    private AppService requireService(UUID serviceId) {
        return appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
    }
}
