package io.nimbus.platform.project.dto;

import io.nimbus.platform.project.domain.ProjectStatus;
import io.nimbus.platform.project.domain.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(min = 3, max = 50) String name,
            @NotNull UUID workspaceId,
            UUID teamId,
            @Size(max = 500) String description,
            Visibility visibility
    ) {
    }

    public record UpdateProjectRequest(
            @Size(min = 3, max = 50) String name,
            @Size(max = 500) String description,
            UUID teamId,
            Visibility visibility
    ) {
    }

    public record ProjectResponse(
            UUID id,
            String name,
            String description,
            ProjectStatus status,
            Visibility visibility,
            UUID workspaceId,
            UUID teamId,
            UUID ownerId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
