package io.nimbus.platform.environment.dto;

import io.nimbus.platform.environment.domain.DeploymentStrategy;
import io.nimbus.platform.environment.domain.EnvironmentStatus;
import io.nimbus.platform.environment.domain.HealthStatus;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EnvironmentDtos {

    private EnvironmentDtos() {
    }

    public record CreateEnvironmentRequest(
            @NotNull EnvironmentType type,
            @Size(max = 63) String namespace,
            @Size(max = 255) String domain,
            @Size(max = 64) String clusterLabel,
            DeploymentStrategy deploymentStrategy,
            @Min(1) @Max(50) Integer replicaCount,
            @Size(max = 32) String cpu,
            @Size(max = 32) String memory,
            Boolean hpaEnabled,
            @Size(max = 100) String gitOpsBranch
    ) {
    }

    public record UpdateEnvironmentRequest(
            @Size(max = 255) String domain,
            DeploymentStrategy deploymentStrategy,
            @Min(1) @Max(50) Integer replicaCount,
            @Size(max = 32) String cpu,
            @Size(max = 32) String memory,
            Boolean hpaEnabled,
            @Size(max = 100) String gitOpsBranch
    ) {
    }

    public record EnvironmentResponse(
            UUID id,
            UUID serviceId,
            UUID projectId,
            UUID workspaceId,
            EnvironmentType type,
            EnvironmentStatus status,
            String namespace,
            String domain,
            String clusterLabel,
            DeploymentStrategy deploymentStrategy,
            Integer replicaCount,
            String cpu,
            String memory,
            Boolean hpaEnabled,
            String gitOpsBranch,
            HealthStatus healthStatus,
            String healthMessage,
            Instant lastHealthAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt
    ) {
    }

    public record EnvironmentHealthResponse(
            UUID environmentId,
            EnvironmentType type,
            EnvironmentStatus status,
            HealthStatus healthStatus,
            String message,
            Instant checkedAt,
            String namespace,
            String clusterLabel
    ) {
    }

    public record EnvironmentListResponse(
            UUID serviceId,
            List<EnvironmentResponse> items,
            int count
    ) {
    }
}
