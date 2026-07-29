package io.nimbus.platform.github.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.crypto.TokenCryptoService;
import io.nimbus.platform.github.domain.ConnectionStatus;
import io.nimbus.platform.github.domain.GitHubConnection;
import io.nimbus.platform.github.dto.GitHubDtos;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.provider.GitUserProfile;
import io.nimbus.platform.github.repository.GitHubConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GitHubConnectionService {

    private final GitHubConnectionRepository connectionRepository;
    private final GitProvider gitProvider;
    private final TokenCryptoService tokenCryptoService;

    public GitHubConnectionService(
            GitHubConnectionRepository connectionRepository,
            GitProvider gitProvider,
            TokenCryptoService tokenCryptoService
    ) {
        this.connectionRepository = connectionRepository;
        this.gitProvider = gitProvider;
        this.tokenCryptoService = tokenCryptoService;
    }

    @Transactional
    public GitHubDtos.ConnectionResponse connect(NimbusPrincipal principal, String personalAccessToken) {
        String token = personalAccessToken != null ? personalAccessToken.trim() : "";
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "personalAccessToken is required");
        }
        GitUserProfile profile = gitProvider.validateToken(token);
        String enc = tokenCryptoService.encrypt(token);

        Optional<GitHubConnection> existing = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId());
        GitHubConnection connection;
        if (existing.isPresent()) {
            connection = existing.get();
            if (connection.getStatus() == ConnectionStatus.CONNECTED
                    && connection.getLogin().equals(profile.login())) {
                // refresh token
                connection.updateToken(enc, profile.login(), profile.avatarUrl(), "repo,workflow,read:user");
            } else {
                connection.updateToken(enc, profile.login(), profile.avatarUrl(), "repo,workflow,read:user");
            }
        } else {
            connection = GitHubConnection.create(
                    principal.userId(),
                    principal.workspaceId(),
                    profile.login(),
                    profile.id(),
                    profile.avatarUrl(),
                    enc,
                    "repo,workflow,read:user"
            );
        }
        connection = connectionRepository.save(connection);
        return toResponse(connection);
    }

    @Transactional(readOnly = true)
    public Optional<GitHubDtos.ConnectionResponse> getConnection(NimbusPrincipal principal) {
        return connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED)
                .map(this::toResponse);
    }

    @Transactional
    public void disconnect(NimbusPrincipal principal) {
        GitHubConnection connection = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));
        connection.disconnect();
        connection.softDelete();
        connectionRepository.save(connection);
    }

    @Transactional(readOnly = true)
    public GitHubDtos.HealthResponse health(NimbusPrincipal principal) {
        Optional<GitHubConnection> opt = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED);
        if (opt.isEmpty()) {
            return new GitHubDtos.HealthResponse(
                    gitProvider.providerName(), "DISCONNECTED", null, null, null, false
            );
        }
        GitHubConnection connection = opt.get();
        try {
            String token = tokenCryptoService.decrypt(connection.getAccessTokenEnc());
            gitProvider.validateToken(token);
            GitProvider.RateLimitStatus rl = gitProvider.rateLimit(token);
            return new GitHubDtos.HealthResponse(
                    gitProvider.providerName(),
                    "UP",
                    connection.getLogin(),
                    rl.remaining(),
                    rl.limit(),
                    true
            );
        } catch (Exception ex) {
            return new GitHubDtos.HealthResponse(
                    gitProvider.providerName(), "ERROR", connection.getLogin(), null, null, true
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<GitHubConnection> findActiveEntity(java.util.UUID userId) {
        return connectionRepository.findByUserIdAndDeletedAtIsNull(userId)
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED);
    }

    public String decryptToken(GitHubConnection connection) {
        return tokenCryptoService.decrypt(connection.getAccessTokenEnc());
    }

    private GitHubDtos.ConnectionResponse toResponse(GitHubConnection c) {
        return new GitHubDtos.ConnectionResponse(
                c.getId(),
                c.getLogin(),
                c.getAvatarUrl(),
                c.getStatus(),
                c.getScopes(),
                c.getLastValidatedAt(),
                c.getCreatedAt()
        );
    }
}
