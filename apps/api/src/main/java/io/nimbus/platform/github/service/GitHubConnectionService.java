package io.nimbus.platform.github.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.GithubProperties;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.TokenStore;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.crypto.TokenCryptoService;
import io.nimbus.platform.github.domain.AuthMethod;
import io.nimbus.platform.github.domain.ConnectionStatus;
import io.nimbus.platform.github.domain.GitHubConnection;
import io.nimbus.platform.github.dto.GitHubDtos;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.provider.GitUserProfile;
import io.nimbus.platform.github.repository.GitHubConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitHubConnectionService {

    private static final Logger log = LoggerFactory.getLogger(GitHubConnectionService.class);
    private static final String SCM_STATE_PREFIX = "oauth:scm:";

    private final GitHubConnectionRepository connectionRepository;
    private final GitProvider gitProvider;
    private final TokenCryptoService tokenCryptoService;
    private final GithubProperties githubProperties;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final AuditService auditService;

    public GitHubConnectionService(
            GitHubConnectionRepository connectionRepository,
            GitProvider gitProvider,
            TokenCryptoService tokenCryptoService,
            GithubProperties githubProperties,
            TokenStore tokenStore,
            ObjectMapper objectMapper,
            AuditService auditService
    ) {
        this.connectionRepository = connectionRepository;
        this.gitProvider = gitProvider;
        this.tokenCryptoService = tokenCryptoService;
        this.githubProperties = githubProperties;
        this.tokenStore = tokenStore;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.restClient = RestClient.builder().build();
    }

    public GitHubDtos.OAuthConfigResponse oauthConfig() {
        return new GitHubDtos.OAuthConfigResponse(
                githubProperties.isConfigured(),
                githubProperties.getScmScopes(),
                "/api/v1/github/oauth/authorize"
        );
    }

    /**
     * OAuth SCM 연결 시작 — authorize URL 반환 (repo 권한).
     */
    public GitHubDtos.OAuthAuthorizeResponse startOAuth(NimbusPrincipal principal) {
        if (!githubProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.AUTH_GITHUB_NOT_CONFIGURED);
        }
        String state = UUID.randomUUID().toString();
        String payload = principal.userId() + "|"
                + (principal.workspaceId() != null ? principal.workspaceId() : "");
        tokenStore.put(SCM_STATE_PREFIX + state, payload, Duration.ofMinutes(10));

        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + urlEncode(githubProperties.getClientId())
                + "&redirect_uri=" + urlEncode(githubProperties.getScmRedirectUri())
                + "&scope=" + urlEncode(githubProperties.getScmScopes())
                + "&state=" + urlEncode(state);
        return new GitHubDtos.OAuthAuthorizeResponse(url, true);
    }

    /**
     * OAuth callback — state 로 사용자 식별, 토큰 저장.
     * @return frontend redirect URL
     */
    @Transactional
    public String handleOAuthCallback(String code, String state) {
        if (!githubProperties.isConfigured()) {
            return frontendScmRedirect(false, "oauth_not_configured");
        }
        String stateKey = SCM_STATE_PREFIX + state;
        Optional<String> payloadOpt = tokenStore.get(stateKey);
        if (payloadOpt.isEmpty()) {
            return frontendScmRedirect(false, "invalid_state");
        }
        tokenStore.delete(stateKey);

        String[] parts = payloadOpt.get().split("\\|", -1);
        UUID userId;
        UUID workspaceId = null;
        try {
            userId = UUID.fromString(parts[0]);
            if (parts.length > 1 && !parts[1].isBlank()) {
                workspaceId = UUID.fromString(parts[1]);
            }
        } catch (Exception e) {
            return frontendScmRedirect(false, "invalid_state_payload");
        }

        try {
            String accessToken = exchangeCode(code, githubProperties.getScmRedirectUri());
            GitUserProfile profile = gitProvider.validateToken(accessToken);
            GitHubConnection connection = saveConnection(
                    userId, workspaceId, profile, accessToken, githubProperties.getScmScopes(), AuthMethod.OAUTH
            );
            auditService.recordRaw(
                    userId,
                    null,
                    profile.login(),
                    AuditAction.CONNECT_GITHUB,
                    "GITHUB_CONNECTION",
                    connection.getId(),
                    profile.login(),
                    workspaceId,
                    null,
                    "GitHub SCM OAuth 연결"
            );
            return frontendScmRedirect(true, null);
        } catch (BusinessException ex) {
            log.warn("SCM OAuth failed: {}", ex.getMessage());
            return frontendScmRedirect(false, ex.getErrorCode().getCode());
        } catch (Exception ex) {
            log.error("SCM OAuth error", ex);
            return frontendScmRedirect(false, "oauth_failed");
        }
    }

    @Transactional
    public GitHubDtos.ConnectionResponse connectPat(NimbusPrincipal principal, String personalAccessToken) {
        String token = personalAccessToken != null ? personalAccessToken.trim() : "";
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "personalAccessToken is required");
        }
        GitUserProfile profile = gitProvider.validateToken(token);
        GitHubConnection connection = saveConnection(
                principal.userId(),
                principal.workspaceId(),
                profile,
                token,
                "repo,workflow,read:user",
                AuthMethod.PAT
        );
        auditService.recordSuccess(
                principal,
                AuditAction.CONNECT_GITHUB,
                "GITHUB_CONNECTION",
                connection.getId(),
                connection.getLogin(),
                principal.workspaceId(),
                "GitHub PAT 연결"
        );
        return toResponse(connection);
    }

    @Transactional(readOnly = true)
    public Optional<GitHubDtos.ConnectionResponse> getConnection(NimbusPrincipal principal) {
        return connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GitHubDtos.StatusResponse status(NimbusPrincipal principal) {
        Optional<GitHubConnection> opt = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED);
        return new GitHubDtos.StatusResponse(
                opt.isPresent(),
                "GitHub",
                githubProperties.isConfigured(),
                opt.map(GitHubConnection::getAuthMethod).orElse(null),
                opt.map(GitHubConnection::getLogin).orElse(null)
        );
    }

    @Transactional
    public void disconnect(NimbusPrincipal principal) {
        GitHubConnection connection = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));
        String login = connection.getLogin();
        UUID connectionId = connection.getId();
        connection.disconnect();
        connectionRepository.save(connection);
        auditService.recordSuccess(
                principal,
                AuditAction.DISCONNECT_GITHUB,
                "GITHUB_CONNECTION",
                connectionId,
                login,
                principal.workspaceId(),
                "GitHub 연결 해제"
        );
    }

    @Transactional(readOnly = true)
    public GitHubDtos.HealthResponse health(NimbusPrincipal principal) {
        Optional<GitHubConnection> opt = connectionRepository.findByUserIdAndDeletedAtIsNull(principal.userId())
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED);
        if (opt.isEmpty()) {
            return new GitHubDtos.HealthResponse(
                    gitProvider.providerName(), "DISCONNECTED", null, null, null, false, null
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
                    true,
                    connection.getAuthMethod()
            );
        } catch (Exception ex) {
            return new GitHubDtos.HealthResponse(
                    gitProvider.providerName(), "ERROR", connection.getLogin(),
                    null, null, true, connection.getAuthMethod()
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<GitHubConnection> findActiveEntity(UUID userId) {
        return connectionRepository.findByUserIdAndDeletedAtIsNull(userId)
                .filter(c -> c.getStatus() == ConnectionStatus.CONNECTED);
    }

    public String decryptToken(GitHubConnection connection) {
        return tokenCryptoService.decrypt(connection.getAccessTokenEnc());
    }

    private GitHubConnection saveConnection(
            UUID userId,
            UUID workspaceId,
            GitUserProfile profile,
            String accessToken,
            String scopes,
            AuthMethod method
    ) {
        String enc = tokenCryptoService.encrypt(accessToken);
        Optional<GitHubConnection> existing = connectionRepository.findByUserIdAndDeletedAtIsNull(userId);
        GitHubConnection connection;
        if (existing.isPresent()) {
            connection = existing.get();
            connection.updateToken(enc, profile.login(), profile.avatarUrl(), scopes, method);
        } else {
            connection = GitHubConnection.create(
                    userId,
                    workspaceId,
                    profile.login(),
                    profile.id(),
                    profile.avatarUrl(),
                    enc,
                    scopes,
                    method
            );
        }
        return connectionRepository.save(connection);
    }

    private String exchangeCode(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubProperties.getClientId());
        form.add("client_secret", githubProperties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", redirectUri);

        String body = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(String.class);

        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("error")) {
                throw new BusinessException(
                        ErrorCode.AUTH_OAUTH_FAILED,
                        node.path("error_description").asText("OAuth error")
                );
            }
            String token = node.path("access_token").asText(null);
            if (token == null || token.isBlank()) {
                throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED, "empty access_token");
            }
            return token;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED, ex.getMessage());
        }
    }

    private String frontendScmRedirect(boolean success, String error) {
        String base = githubProperties.getScmFrontendCallback();
        if (success) {
            return base + (base.contains("?") ? "&" : "?") + "scm=connected";
        }
        String err = error != null ? error : "unknown";
        return base + (base.contains("?") ? "&" : "?") + "scm=error&reason=" + urlEncode(err);
    }

    private GitHubDtos.ConnectionResponse toResponse(GitHubConnection c) {
        return new GitHubDtos.ConnectionResponse(
                c.getId(),
                c.getLogin(),
                c.getAvatarUrl(),
                c.getStatus(),
                c.getScopes(),
                c.getAuthMethod(),
                c.getLastValidatedAt(),
                c.getCreatedAt()
        );
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
