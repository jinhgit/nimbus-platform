package io.nimbus.platform.auth.dto;

import io.nimbus.platform.auth.domain.GlobalRole;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record DevLoginRequest(
            @NotBlank String name,
            @NotBlank @Email String email
    ) {
    }

    public record WorkspaceSwitchRequest(
            @NotNull UUID workspaceId
    ) {
    }

    public record UserSummary(
            UUID id,
            String name,
            String email,
            String avatarUrl,
            GlobalRole role,
            UUID workspaceId
    ) {
    }

    public record WorkspaceSummary(
            UUID id,
            String name,
            String slug
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserSummary user
    ) {
    }

    public record MeResponse(
            UUID id,
            String name,
            String email,
            String avatarUrl,
            GlobalRole role,
            WorkspaceSummary workspace,
            WorkspaceRole workspaceRole,
            boolean canMutate
    ) {
    }

    public record TokenValidateResponse(
            boolean valid,
            long expiresIn
    ) {
    }

    public record PermissionsResponse(
            List<String> permissions,
            WorkspaceRole workspaceRole,
            boolean canMutate
    ) {
    }

    public record GithubLoginResponse(
            String authorizeUrl
    ) {
    }
}
