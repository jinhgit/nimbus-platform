package io.nimbus.platform.pipeline.dto;

import io.nimbus.platform.pipeline.domain.PipelineStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class PipelineDtos {

    private PipelineDtos() {
    }

    public record CreatePipelineRequest(
            @NotNull UUID serviceId
    ) {
    }

    public record PipelineResponse(
            UUID id,
            UUID serviceId,
            UUID projectId,
            UUID workspaceId,
            String serviceName,
            String name,
            PipelineStatus status,
            Integer progress,
            String currentStep,
            String imageTag,
            String dockerfilePath,
            Instant startedAt,
            Instant finishedAt,
            Instant createdAt
    ) {
    }

    public record PipelineLogsResponse(
            UUID id,
            PipelineStatus status,
            Integer progress,
            String currentStep,
            String logs
    ) {
    }
}
