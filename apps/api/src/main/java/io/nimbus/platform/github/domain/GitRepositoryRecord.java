package io.nimbus.platform.github.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "git_repositories")
public class GitRepositoryRecord extends BaseEntity {

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "wizard_id")
    private UUID wizardId;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(name = "repo_name", nullable = false, length = 100)
    private String repoName;

    @Column(name = "html_url", length = 500)
    private String htmlUrl;

    @Column(name = "clone_url", length = 500)
    private String cloneUrl;

    @Column(name = "default_branch", length = 50)
    private String defaultBranch = "main";

    @Column(name = "github_repo_id", length = 64)
    private String githubRepoId;

    @Column(length = 16)
    private String visibility = "PRIVATE";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RepoStatus status = RepoStatus.REQUESTED;

    protected GitRepositoryRecord() {
    }

    public static GitRepositoryRecord create(
            UUID connectionId,
            UUID userId,
            UUID workspaceId,
            UUID projectId,
            UUID wizardId,
            String owner,
            String repoName,
            String visibility
    ) {
        GitRepositoryRecord r = new GitRepositoryRecord();
        r.connectionId = connectionId;
        r.userId = userId;
        r.workspaceId = workspaceId;
        r.projectId = projectId;
        r.wizardId = wizardId;
        r.owner = owner;
        r.repoName = repoName;
        r.visibility = visibility != null ? visibility : "PRIVATE";
        r.status = RepoStatus.REQUESTED;
        return r;
    }

    public void markCreated(String githubRepoId, String htmlUrl, String cloneUrl, String defaultBranch) {
        this.githubRepoId = githubRepoId;
        this.htmlUrl = htmlUrl;
        this.cloneUrl = cloneUrl;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.status = RepoStatus.CREATED;
    }

    public void markReady() {
        this.status = RepoStatus.READY;
    }

    public void markFailed() {
        this.status = RepoStatus.FAILED;
    }

    public void bindService(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public void setStatus(RepoStatus status) {
        this.status = status;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getCloneUrl() {
        return cloneUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getGithubRepoId() {
        return githubRepoId;
    }

    public String getVisibility() {
        return visibility;
    }

    public RepoStatus getStatus() {
        return status;
    }
}
