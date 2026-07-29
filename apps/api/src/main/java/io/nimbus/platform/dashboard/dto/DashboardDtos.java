package io.nimbus.platform.dashboard.dto;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.environment.domain.PromotionStatus;
import io.nimbus.platform.provision.domain.SagaStatus;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.workspace.domain.WorkspaceRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record OverviewResponse(
            UUID workspaceId,
            WorkspaceRole workspaceRole,
            boolean canMutate,
            Counts counts,
            List<PromoteItem> recentPromotes,
            List<SagaItem> failedSagas,
            List<AuditItem> recentAudits
    ) {
    }

    public record Counts(
            long projects,
            long services,
            long environments,
            long readyServices,
            long failedSagas,
            long auditEvents
    ) {
    }

    public record PromoteItem(
            UUID id,
            UUID serviceId,
            EnvironmentType sourceType,
            EnvironmentType targetType,
            PromotionStatus status,
            String message,
            Instant at
    ) {
    }

    public record SagaItem(
            UUID id,
            UUID wizardId,
            Integer attempt,
            SagaStatus status,
            String failureReason,
            Instant at
    ) {
    }

    public record AuditItem(
            UUID id,
            AuditAction action,
            String resourceType,
            String resourceName,
            String actorName,
            Instant at
    ) {
    }
}
