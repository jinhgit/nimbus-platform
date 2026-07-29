package io.nimbus.platform.deployment.dto;

import io.nimbus.platform.deployment.domain.DeploymentStatus;
import io.nimbus.platform.deployment.domain.DeploymentTrigger;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DeploymentDtos {

    private DeploymentDtos() {
    }

    public record DeploymentResponse(
            UUID id,
            UUID serviceId,
            UUID environmentId,
            EnvironmentType environmentType,
            DeploymentStatus status,
            DeploymentTrigger trigger,
            String versionLabel,
            String imageTag,
            String namespaceName,
            String message,
            UUID promotionId,
            UUID wizardId,
            UUID pipelineId,
            UUID triggeredBy,
            Instant createdAt,
            Instant finishedAt
    ) {
    }

    public record DeploymentListResponse(
            UUID serviceId,
            List<DeploymentResponse> items,
            int count
    ) {
    }

    public record TimelineItem(
            String kind,
            UUID id,
            Instant at,
            String title,
            String detail,
            String status
    ) {
    }

    public record TimelineResponse(
            UUID serviceId,
            List<TimelineItem> items
    ) {
    }
}
