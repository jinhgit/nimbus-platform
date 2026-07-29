package io.nimbus.platform.audit.dto;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.domain.AuditResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            UUID id,
            UUID actorId,
            String actorEmail,
            String actorName,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String resourceName,
            UUID workspaceId,
            AuditResult result,
            String message,
            String ipAddress,
            String userAgent,
            Instant createdAt
    ) {
    }

    public record AuditListResponse(
            List<AuditLogResponse> items,
            int count,
            int limit
    ) {
    }
}
