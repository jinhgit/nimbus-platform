package io.nimbus.platform.environment.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Service 의 Infrastructure Context (DEV / STAGE / PRODUCTION).
 * Aggregate Root — Variable/Secret/Promote 는 이후 스프린트.
 */
@Entity
@Table(name = "service_environments")
public class ServiceEnvironment extends BaseEntity {
    // type uniqueness is enforced in service for non-deleted rows (soft-delete safe)

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnvironmentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnvironmentStatus status = EnvironmentStatus.CREATING;

    @Column(nullable = false, length = 63)
    private String namespaceName;

    @Column(length = 255)
    private String domain;

    /** free-only: 클러스터 라벨 (k3d/kind/local/none) */
    @Column(name = "cluster_label", length = 64)
    private String clusterLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_strategy", nullable = false, length = 16)
    private DeploymentStrategy deploymentStrategy = DeploymentStrategy.ROLLING;

    @Column(name = "replica_count")
    private Integer replicaCount = 1;

    @Column(length = 32)
    private String cpu;

    @Column(length = 32)
    private String memory;

    @Column(name = "hpa_enabled")
    private Boolean hpaEnabled = false;

    @Column(name = "git_ops_branch", length = 100)
    private String gitOpsBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 16)
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "health_message", length = 500)
    private String healthMessage;

    @Column(name = "last_health_at")
    private Instant lastHealthAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected ServiceEnvironment() {
    }

    public static ServiceEnvironment create(
            UUID serviceId,
            UUID projectId,
            UUID workspaceId,
            EnvironmentType type,
            String namespaceName,
            String domain,
            String clusterLabel,
            DeploymentStrategy strategy,
            Integer replicaCount,
            String cpu,
            String memory,
            Boolean hpaEnabled,
            String gitOpsBranch,
            UUID createdBy
    ) {
        ServiceEnvironment env = new ServiceEnvironment();
        env.serviceId = serviceId;
        env.projectId = projectId;
        env.workspaceId = workspaceId;
        env.type = type;
        env.namespaceName = namespaceName;
        env.domain = domain;
        env.clusterLabel = clusterLabel != null ? clusterLabel : "local";
        env.deploymentStrategy = strategy != null ? strategy : DeploymentStrategy.ROLLING;
        env.replicaCount = replicaCount != null ? replicaCount : 1;
        env.cpu = cpu;
        env.memory = memory;
        env.hpaEnabled = hpaEnabled != null && hpaEnabled;
        env.gitOpsBranch = gitOpsBranch != null ? gitOpsBranch : defaultBranch(type);
        env.createdBy = createdBy;
        env.status = EnvironmentStatus.READY;
        env.healthStatus = HealthStatus.UNKNOWN;
        return env;
    }

    public static String defaultBranch(EnvironmentType type) {
        return switch (type) {
            case DEV -> "develop";
            case STAGE -> "staging";
            case PRODUCTION -> "main";
        };
    }

    public void update(
            String domain,
            DeploymentStrategy strategy,
            Integer replicaCount,
            String cpu,
            String memory,
            Boolean hpaEnabled,
            String gitOpsBranch
    ) {
        if (domain != null) {
            this.domain = domain.isBlank() ? null : domain;
        }
        if (strategy != null) {
            this.deploymentStrategy = strategy;
        }
        if (replicaCount != null) {
            this.replicaCount = replicaCount;
        }
        if (cpu != null) {
            this.cpu = cpu.isBlank() ? null : cpu;
        }
        if (memory != null) {
            this.memory = memory.isBlank() ? null : memory;
        }
        if (hpaEnabled != null) {
            this.hpaEnabled = hpaEnabled;
        }
        if (gitOpsBranch != null && !gitOpsBranch.isBlank()) {
            this.gitOpsBranch = gitOpsBranch;
        }
    }

    public void markReady() {
        this.status = EnvironmentStatus.READY;
    }

    public void markDeploying() {
        this.status = EnvironmentStatus.DEPLOYING;
    }

    public void markFailed(String message) {
        this.status = EnvironmentStatus.FAILED;
        this.healthStatus = HealthStatus.UNHEALTHY;
        this.healthMessage = message;
        this.lastHealthAt = Instant.now();
    }

    public void archive() {
        this.status = EnvironmentStatus.ARCHIVED;
        this.archivedAt = Instant.now();
    }

    public void restoreFromArchive() {
        this.status = EnvironmentStatus.READY;
        this.archivedAt = null;
        restore(); // BaseEntity soft-delete restore if needed
    }

    public void applyHealth(HealthStatus status, String message) {
        this.healthStatus = status != null ? status : HealthStatus.UNKNOWN;
        this.healthMessage = message;
        this.lastHealthAt = Instant.now();
    }

    public void bindCluster(String clusterLabel, String namespaceName) {
        if (clusterLabel != null) {
            this.clusterLabel = clusterLabel;
        }
        if (namespaceName != null && !namespaceName.isBlank()) {
            this.namespaceName = namespaceName;
        }
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public EnvironmentType getType() {
        return type;
    }

    public EnvironmentStatus getStatus() {
        return status;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public String getDomain() {
        return domain;
    }

    public String getClusterLabel() {
        return clusterLabel;
    }

    public DeploymentStrategy getDeploymentStrategy() {
        return deploymentStrategy;
    }

    public Integer getReplicaCount() {
        return replicaCount;
    }

    public String getCpu() {
        return cpu;
    }

    public String getMemory() {
        return memory;
    }

    public Boolean getHpaEnabled() {
        return hpaEnabled;
    }

    public String getGitOpsBranch() {
        return gitOpsBranch;
    }

    public HealthStatus getHealthStatus() {
        return healthStatus;
    }

    public String getHealthMessage() {
        return healthMessage;
    }

    public Instant getLastHealthAt() {
        return lastHealthAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
