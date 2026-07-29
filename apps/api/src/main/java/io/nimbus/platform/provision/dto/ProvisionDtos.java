package io.nimbus.platform.provision.dto;

import io.nimbus.platform.provision.domain.SagaStatus;
import io.nimbus.platform.provision.domain.StepCode;
import io.nimbus.platform.provision.domain.StepStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProvisionDtos {

    private ProvisionDtos() {
    }

    public record SagaStepResponse(
            UUID id,
            StepCode stepCode,
            String name,
            Integer stepOrder,
            StepStatus status,
            String message,
            String compensationMessage,
            Instant startedAt,
            Instant finishedAt
    ) {
    }

    public record SagaResponse(
            UUID id,
            UUID wizardId,
            Integer attempt,
            SagaStatus status,
            String currentStepCode,
            String failureReason,
            String compensationLog,
            Instant startedAt,
            Instant finishedAt,
            List<SagaStepResponse> steps
    ) {
    }

    public record RetryResponse(
            UUID wizardId,
            UUID sagaId,
            Integer attempt,
            SagaStatus status
    ) {
    }
}
