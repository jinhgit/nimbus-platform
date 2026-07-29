package io.nimbus.platform.github.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "github_connections")
public class GitHubConnection extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(nullable = false, length = 100)
    private String login;

    @Column(name = "github_user_id", length = 64)
    private String githubUserId;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Lob
    @Column(name = "access_token_enc", nullable = false)
    private String accessTokenEnc;

    @Column(length = 128)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConnectionStatus status = ConnectionStatus.CONNECTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 16)
    private AuthMethod authMethod = AuthMethod.PAT;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    protected GitHubConnection() {
    }

    public static GitHubConnection create(
            UUID userId,
            UUID workspaceId,
            String login,
            String githubUserId,
            String avatarUrl,
            String accessTokenEnc,
            String scopes,
            AuthMethod authMethod
    ) {
        GitHubConnection c = new GitHubConnection();
        c.userId = userId;
        c.workspaceId = workspaceId;
        c.login = login;
        c.githubUserId = githubUserId;
        c.avatarUrl = avatarUrl;
        c.accessTokenEnc = accessTokenEnc;
        c.scopes = scopes;
        c.authMethod = authMethod != null ? authMethod : AuthMethod.PAT;
        c.status = ConnectionStatus.CONNECTED;
        c.lastValidatedAt = Instant.now();
        return c;
    }

    public void updateToken(
            String accessTokenEnc,
            String login,
            String avatarUrl,
            String scopes,
            AuthMethod authMethod
    ) {
        this.accessTokenEnc = accessTokenEnc;
        this.login = login;
        this.avatarUrl = avatarUrl;
        this.scopes = scopes;
        if (authMethod != null) {
            this.authMethod = authMethod;
        }
        this.status = ConnectionStatus.CONNECTED;
        this.lastValidatedAt = Instant.now();
        this.restore();
    }

    public void disconnect() {
        this.status = ConnectionStatus.DISCONNECTED;
        this.accessTokenEnc = "";
        // softDelete 하지 않음 — user_id unique 유지 후 재연결 가능
    }

    public void markValidated() {
        this.lastValidatedAt = Instant.now();
        this.status = ConnectionStatus.CONNECTED;
    }

    public void markError() {
        this.status = ConnectionStatus.ERROR;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getLogin() {
        return login;
    }

    public String getGithubUserId() {
        return githubUserId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public String getScopes() {
        return scopes;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public AuthMethod getAuthMethod() {
        return authMethod;
    }

    public Instant getLastValidatedAt() {
        return lastValidatedAt;
    }
}
