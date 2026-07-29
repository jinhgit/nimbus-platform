package io.nimbus.platform.serviceapp.domain;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "app_services",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "name"})
)
public class AppService extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RuntimeType runtime;

    @Column(name = "template_id")
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ServiceStatus status = ServiceStatus.CREATING;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 16)
    private EnvironmentType environmentType = EnvironmentType.DEV;

    @Column(name = "replica_count")
    private Integer replicaCount = 1;

    @Column(length = 32)
    private String databaseType;

    @Column(length = 32)
    private String cacheType;

    @Column
    private Boolean hpaEnabled = false;

    @Column(name = "wizard_id")
    private UUID wizardId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "github_repo_url", length = 500)
    private String githubRepoUrl;

    @Column(name = "github_owner", length = 100)
    private String githubOwner;

    @Column(name = "github_repo_name", length = 100)
    private String githubRepoName;

    @Column(name = "k8s_namespace", length = 63)
    private String k8sNamespace;

    @Column(name = "k8s_deployment", length = 63)
    private String k8sDeployment;

    @Column(name = "k8s_status", length = 16)
    private String k8sStatus;

    @Column(name = "k8s_cluster_type", length = 32)
    private String k8sClusterType;

    /** Comma-separated tags, e.g. "payment,prod,critical" */
    @Column(length = 400)
    private String tags;

    protected AppService() {
    }

    public static AppService create(
            UUID projectId,
            UUID workspaceId,
            String name,
            String description,
            RuntimeType runtime,
            UUID templateId,
            EnvironmentType environmentType,
            Integer replicaCount,
            String databaseType,
            String cacheType,
            Boolean hpaEnabled,
            UUID wizardId,
            UUID ownerId
    ) {
        AppService s = new AppService();
        s.projectId = projectId;
        s.workspaceId = workspaceId;
        s.name = name;
        s.description = description;
        s.runtime = runtime;
        s.templateId = templateId;
        s.environmentType = environmentType != null ? environmentType : EnvironmentType.DEV;
        s.replicaCount = replicaCount != null ? replicaCount : 1;
        s.databaseType = databaseType;
        s.cacheType = cacheType;
        s.hpaEnabled = hpaEnabled != null && hpaEnabled;
        s.wizardId = wizardId;
        s.ownerId = ownerId;
        s.status = ServiceStatus.CREATING;
        return s;
    }

    public void markReady() {
        this.status = ServiceStatus.READY;
    }

    public void markFailed() {
        this.status = ServiceStatus.FAILED;
    }

    public void bindGitHub(String owner, String repoName, String htmlUrl) {
        this.githubOwner = owner;
        this.githubRepoName = repoName;
        this.githubRepoUrl = htmlUrl;
    }

    public void bindK8s(String namespace, String deployment, String status, String clusterType) {
        this.k8sNamespace = namespace;
        this.k8sDeployment = deployment;
        this.k8sStatus = status;
        this.k8sClusterType = clusterType;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public RuntimeType getRuntime() {
        return runtime;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public Integer getReplicaCount() {
        return replicaCount;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getCacheType() {
        return cacheType;
    }

    public Boolean getHpaEnabled() {
        return hpaEnabled;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getGithubRepoUrl() {
        return githubRepoUrl;
    }

    public String getGithubOwner() {
        return githubOwner;
    }

    public String getGithubRepoName() {
        return githubRepoName;
    }

    public String getK8sNamespace() {
        return k8sNamespace;
    }

    public String getK8sDeployment() {
        return k8sDeployment;
    }

    public String getTags() {
        return tags;
    }

    public String getK8sStatus() {
        return k8sStatus;
    }

    public String getK8sClusterType() {
        return k8sClusterType;
    }
}
