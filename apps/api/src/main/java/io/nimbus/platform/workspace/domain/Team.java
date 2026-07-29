package io.nimbus.platform.workspace.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    protected Team() {
    }

    public static Team create(UUID workspaceId, String name, String description) {
        Team team = new Team();
        team.workspaceId = workspaceId;
        team.name = name;
        team.description = description;
        return team;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
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
}
