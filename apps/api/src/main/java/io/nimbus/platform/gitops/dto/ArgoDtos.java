package io.nimbus.platform.gitops.dto;

import java.util.UUID;

public final class ArgoDtos {

    private ArgoDtos() {
    }

    public record ArgoSyncResponse(
            UUID serviceId,
            String serviceName,
            String mode,
            String syncStatus,
            String healthStatus,
            String applicationName,
            String namespace,
            String repoUrl,
            String targetRevision,
            String message,
            String applicationManifest
    ) {
    }
}
