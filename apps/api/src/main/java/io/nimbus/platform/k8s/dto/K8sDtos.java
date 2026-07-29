package io.nimbus.platform.k8s.dto;

import io.nimbus.platform.k8s.domain.DeployStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class K8sDtos {

    private K8sDtos() {
    }

    public record ClusterStatusResponse(
            boolean available,
            boolean enabled,
            String context,
            String clusterType,
            String version,
            String message,
            int nodeCount,
            int namespaceCount
    ) {
    }

    public record PodSummary(
            String name,
            String phase,
            String node,
            int restarts,
            boolean ready
    ) {
    }

    public record DeploymentResponse(
            UUID id,
            UUID serviceId,
            UUID wizardId,
            String namespaceName,
            String deploymentName,
            String image,
            Integer replicas,
            Integer readyReplicas,
            String clusterContext,
            String clusterType,
            DeployStatus status,
            String message,
            Instant createdAt,
            List<PodSummary> pods
    ) {
    }

    public record DeployResult(
            boolean real,
            String namespace,
            String deployment,
            String image,
            int replicas,
            int readyReplicas,
            String context,
            String clusterType,
            DeployStatus status,
            String message
    ) {
    }
}
