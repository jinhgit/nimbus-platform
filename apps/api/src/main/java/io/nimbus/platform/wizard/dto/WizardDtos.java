package io.nimbus.platform.wizard.dto;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.wizard.domain.WizardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WizardDtos {

    private WizardDtos() {
    }

    public record CreateWizardRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(min = 3, max = 50) String serviceName,
            UUID templateId
    ) {
    }

    public record UpdateWizardRequest(
            @Size(min = 3, max = 50) String serviceName,
            UUID templateId,
            RuntimeType runtime,
            EnvironmentType environmentType,
            String databaseType,
            String cacheType,
            Integer replicaCount,
            Boolean hpaEnabled,
            String cpu,
            String memory,
            String domain,
            Integer currentStep
    ) {
    }

    public record WizardResponse(
            UUID id,
            UUID projectId,
            UUID workspaceId,
            String serviceName,
            UUID templateId,
            RuntimeType runtime,
            EnvironmentType environmentType,
            String databaseType,
            String cacheType,
            Integer replicaCount,
            Boolean hpaEnabled,
            String cpu,
            String memory,
            String domain,
            WizardStatus status,
            Integer currentStep,
            Integer progress,
            String progressMessage,
            UUID serviceId,
            Object recommendation,
            Object preview,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ValidateResponse(
            boolean valid,
            List<String> warnings,
            List<String> errors
    ) {
    }

    public record PreviewResponse(
            String repository,
            String runtime,
            String environment,
            Map<String, String> repositoryStructure,
            String blueprint,
            String helmValues,
            String terraformVars,
            String githubActions,
            String deploymentYaml,
            String argoApplication
    ) {
    }

    public record ExecuteResponse(
            UUID wizardId,
            UUID jobId,
            WizardStatus status,
            Integer progress
    ) {
    }

    public record WizardLogsResponse(
            UUID wizardId,
            WizardStatus status,
            Integer progress,
            String progressMessage,
            String logs
    ) {
    }
}
