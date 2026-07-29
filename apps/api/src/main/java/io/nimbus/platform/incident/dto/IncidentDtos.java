package io.nimbus.platform.incident.dto;

import io.nimbus.platform.incident.domain.IncidentSeverity;
import io.nimbus.platform.incident.domain.IncidentSource;
import io.nimbus.platform.incident.domain.IncidentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IncidentDtos {

    private IncidentDtos() {
    }

    public record IncidentResponse(
            UUID id,
            UUID workspaceId,
            UUID serviceId,
            String serviceName,
            IncidentSource sourceType,
            UUID sourceId,
            String title,
            IncidentSeverity severity,
            IncidentStatus status,
            String summary,
            String analysisText,
            String provider,
            Instant openedAt,
            Instant resolvedAt
    ) {
    }

    public record ScanResponse(
            int opened,
            int scanned,
            List<IncidentResponse> created
    ) {
    }

    public record CountsResponse(
            long open,
            long acknowledged,
            long resolved
    ) {
    }
}
