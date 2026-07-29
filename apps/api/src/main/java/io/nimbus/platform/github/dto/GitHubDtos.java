package io.nimbus.platform.github.dto;

import io.nimbus.platform.github.domain.ConnectionStatus;
import io.nimbus.platform.github.domain.RepoStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class GitHubDtos {

    private GitHubDtos() {
    }

    public record ConnectRequest(
            @NotBlank String personalAccessToken
    ) {
    }

    public record ConnectionResponse(
            UUID id,
            String login,
            String avatarUrl,
            ConnectionStatus status,
            String scopes,
            Instant lastValidatedAt,
            Instant connectedAt
    ) {
    }

    public record HealthResponse(
            String provider,
            String status,
            String login,
            Integer rateLimitRemaining,
            Integer rateLimitLimit,
            boolean connected
    ) {
    }

    public record CreateRepoRequest(
            @NotBlank String repository,
            String description,
            String visibility,
            UUID projectId,
            UUID wizardId
    ) {
    }

    public record RepositoryResponse(
            UUID id,
            String owner,
            String repoName,
            String htmlUrl,
            String cloneUrl,
            String defaultBranch,
            String visibility,
            RepoStatus status,
            UUID serviceId,
            UUID wizardId,
            Instant createdAt
    ) {
    }
}
