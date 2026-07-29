package io.nimbus.platform.workspace.dto;

import io.nimbus.platform.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class WorkspaceDtos {

    private WorkspaceDtos() {
    }

    public record CreateWorkspaceRequest(
            @NotBlank @Size(min = 3, max = 50) String name,
            @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug,
            @Size(max = 300) String description
    ) {
    }

    public record UpdateWorkspaceRequest(
            @Size(min = 3, max = 50) String name,
            @Size(max = 300) String description
    ) {
    }

    public record CreateTeamRequest(
            @NotBlank @Size(min = 2, max = 50) String name,
            @Size(max = 200) String description
    ) {
    }

    public record UpdateTeamRequest(
            @Size(min = 2, max = 50) String name,
            @Size(max = 200) String description
    ) {
    }

    public record InviteMemberRequest(
            @NotBlank String email,
            WorkspaceRole role
    ) {
    }

    public record UpdateMemberRoleRequest(
            WorkspaceRole role
    ) {
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            String slug,
            String description,
            UUID ownerId,
            long memberCount,
            long projectCount,
            WorkspaceRole myRole
    ) {
    }

    public record WorkspaceSummary(
            UUID id,
            String name,
            String slug,
            WorkspaceRole myRole
    ) {
    }

    public record MemberResponse(
            UUID id,
            UUID userId,
            String name,
            String email,
            WorkspaceRole role
    ) {
    }

    public record TeamResponse(
            UUID id,
            UUID workspaceId,
            String name,
            String description
    ) {
    }
}
