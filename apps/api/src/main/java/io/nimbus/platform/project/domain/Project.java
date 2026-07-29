package io.nimbus.platform.project.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"})
)
public class Project extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectStatus status = ProjectStatus.READY;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Project() {
    }

    public static Project create(
            UUID workspaceId,
            UUID teamId,
            String name,
            String description,
            Visibility visibility,
            UUID ownerId
    ) {
        Project project = new Project();
        project.workspaceId = workspaceId;
        project.teamId = teamId;
        project.name = name;
        project.description = description;
        project.visibility = visibility != null ? visibility : Visibility.PRIVATE;
        project.ownerId = ownerId;
        project.status = ProjectStatus.READY;
        return project;
    }

    public void update(String name, String description, UUID teamId, Visibility visibility) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (teamId != null) {
            this.teamId = teamId;
        }
        if (visibility != null) {
            this.visibility = visibility;
        }
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
        this.archivedAt = Instant.now();
    }

    public void restore() {
        this.status = ProjectStatus.READY;
        this.archivedAt = null;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
