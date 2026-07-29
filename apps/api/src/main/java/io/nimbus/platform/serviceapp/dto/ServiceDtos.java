package io.nimbus.platform.serviceapp.dto;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.domain.ServiceStatus;

import java.time.Instant;
import java.util.UUID;

public final class ServiceDtos {

    private ServiceDtos() {
    }

    public record ServiceResponse(
            UUID id,
            String name,
            String description,
            RuntimeType runtime,
            ServiceStatus status,
            EnvironmentType environmentType,
            Integer replicaCount,
            String databaseType,
            String cacheType,
            Boolean hpaEnabled,
            UUID projectId,
            UUID workspaceId,
            UUID templateId,
            UUID wizardId,
            String githubRepoUrl,
            String githubOwner,
            String githubRepoName,
            String k8sNamespace,
            String k8sDeployment,
            String k8sStatus,
            String k8sClusterType,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
