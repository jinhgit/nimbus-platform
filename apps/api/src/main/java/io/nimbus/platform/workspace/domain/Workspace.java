package io.nimbus.platform.workspace.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    @Column(length = 300)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    protected Workspace() {
    }

    public static Workspace create(String name, String slug, String description, UUID ownerId) {
        Workspace workspace = new Workspace();
        workspace.name = name;
        workspace.slug = slug;
        workspace.description = description;
        workspace.ownerId = ownerId;
        return workspace;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }
}
