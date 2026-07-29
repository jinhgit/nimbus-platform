package io.nimbus.platform.observability.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ObservabilityDtos {

    private ObservabilityDtos() {
    }

    public record StackLinks(
            String prometheusUrl,
            String grafanaUrl,
            String lokiUrl,
            boolean prometheusUp,
            boolean grafanaUp,
            String mode
    ) {
    }

    public record MetricPoint(String name, double value, String unit) {
    }

    public record ServiceMetrics(
            UUID serviceId,
            String serviceName,
            String source,
            List<MetricPoint> metrics,
            Instant collectedAt
    ) {
    }

    public record MonitoringOverview(
            StackLinks links,
            int serviceCount,
            int runningDeployments,
            double avgCpu,
            double avgMemory,
            List<ServiceMetrics> topServices
    ) {
    }

    public record LogLine(
            Instant timestamp,
            String level,
            String pod,
            String message
    ) {
    }

    public record LogSnapshot(
            UUID serviceId,
            String serviceName,
            String source,
            List<LogLine> lines
    ) {
    }
}
