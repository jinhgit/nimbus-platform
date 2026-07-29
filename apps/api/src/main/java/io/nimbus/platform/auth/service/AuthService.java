package io.nimbus.platform.auth.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.domain.GlobalRole;
import io.nimbus.platform.auth.domain.User;
import io.nimbus.platform.auth.domain.UserStatus;
import io.nimbus.platform.auth.dto.AuthDtos;
import io.nimbus.platform.auth.repository.UserRepository;
import io.nimbus.platform.auth.security.GithubProperties;
import io.nimbus.platform.auth.security.JwtProperties;
import io.nimbus.platform.auth.security.JwtTokenService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.TokenStore;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.workspace.domain.Workspace;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.repository.WorkspaceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final JwtTokenService jwtTokenService;
    private final TokenStore tokenStore;
    private final JwtProperties jwtProperties;
    private final GithubProperties githubProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            JwtTokenService jwtTokenService,
            TokenStore tokenStore,
            JwtProperties jwtProperties,
            GithubProperties githubProperties,
            ObjectMapper objectMapper,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.jwtTokenService = jwtTokenService;
        this.tokenStore = tokenStore;
        this.jwtProperties = jwtProperties;
        this.githubProperties = githubProperties;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.restClient = RestClient.builder().build();
    }

    @Transactional
    public AuthDtos.LoginResponse devLogin(AuthDtos.DevLoginRequest request) {
        if (!jwtProperties.isDevLoginEnabled()) {
            throw new BusinessException(ErrorCode.AUTH_DEV_DISABLED);
        }
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseGet(() -> {
                    User created = User.createLocal(request.email(), request.name());
                    return userRepository.save(created);
                });
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUTH_USER_DISABLED);
        }
        ensureWorkspace(user);
        user.markLogin();
        userRepository.save(user);
        AuthDtos.LoginResponse tokens = issueTokens(user);
        auditService.recordRaw(
                user.getId(),
                user.getEmail(),
                user.getName(),
                AuditAction.LOGIN,
                "USER",
                user.getId(),
                user.getEmail(),
                user.getCurrentWorkspaceId(),
                null,
                "Dev Login"
        );
        return tokens;
    }

    public AuthDtos.GithubLoginResponse startGithubLogin() {
        if (!githubProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.AUTH_GITHUB_NOT_CONFIGURED);
        }
        String state = UUID.randomUUID().toString();
        tokenStore.put("oauth:state:" + state, "1", Duration.ofMinutes(5));
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + urlEncode(githubProperties.getClientId())
                + "&redirect_uri=" + urlEncode(githubProperties.getRedirectUri())
                + "&scope=" + urlEncode("read:user user:email")
                + "&state=" + urlEncode(state);
        return new AuthDtos.GithubLoginResponse(url);
    }

    @Transactional
    public AuthDtos.LoginResponse handleGithubCallback(String code, String state) {
        if (!githubProperties.isConfigured()) {
            throw new BusinessException(ErrorCode.AUTH_GITHUB_NOT_CONFIGURED);
        }
        String stateKey = "oauth:state:" + state;
        if (tokenStore.get(stateKey).isEmpty()) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_STATE);
        }
        tokenStore.delete(stateKey);

        try {
            String accessToken = exchangeGithubCode(code);
            JsonNode profile = fetchGithubUser(accessToken);
            String githubId = profile.path("id").asText();
            String login = profile.path("login").asText("github-user");
            String name = profile.path("name").asText(login);
            String avatar = profile.path("avatar_url").asText(null);
            String email = profile.path("email").asText(null);
            if (email == null || email.isBlank()) {
                email = fetchPrimaryEmail(accessToken);
            }
            if (email == null || email.isBlank()) {
                email = login + "@users.noreply.github.com";
            }

            final String finalEmail = email;
            final String finalName = name;
            final String finalAvatar = avatar;
            User user = userRepository.findByGithubIdAndDeletedAtIsNull(githubId)
                    .or(() -> userRepository.findByEmailAndDeletedAtIsNull(finalEmail))
                    .orElseGet(() -> userRepository.save(
                            User.createFromGitHub(githubId, finalEmail, finalName, finalAvatar)
                    ));
            user.updateProfile(name, avatar);
            ensureWorkspace(user);
            user.markLogin();
            userRepository.save(user);
            AuthDtos.LoginResponse tokens = issueTokens(user);
            auditService.recordRaw(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    AuditAction.LOGIN,
                    "USER",
                    user.getId(),
                    user.getEmail(),
                    user.getCurrentWorkspaceId(),
                    null,
                    "GitHub OAuth Login"
            );
            return tokens;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("GitHub OAuth failed", ex);
            throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
        String userIdRaw = tokenStore.get("refresh:" + refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REFRESH_EXPIRED));
        tokenStore.delete("refresh:" + refreshToken);
        User user = userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(userIdRaw))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return issueTokens(user);
    }

    public void logout(NimbusPrincipal principal, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            tokenStore.delete("refresh:" + refreshToken);
        }
        if (principal != null) {
            auditService.recordSuccess(
                    principal,
                    AuditAction.LOGOUT,
                    "USER",
                    principal.userId(),
                    principal.email(),
                    principal.workspaceId(),
                    "Logout"
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(NimbusPrincipal principal) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        AuthDtos.WorkspaceSummary workspace = null;
        WorkspaceRole workspaceRole = null;
        boolean canMutate = false;
        if (user.getCurrentWorkspaceId() != null) {
            workspace = workspaceRepository.findByIdAndDeletedAtIsNull(user.getCurrentWorkspaceId())
                    .map(w -> new AuthDtos.WorkspaceSummary(w.getId(), w.getName(), w.getSlug()))
                    .orElse(null);
            if (workspace != null) {
                try {
                    WorkspaceMember member = workspaceBootstrapService.requireMember(
                            user.getCurrentWorkspaceId(), user.getId());
                    workspaceRole = member.getRole();
                    canMutate = WorkspacePermissionService.CAN_MUTATE.contains(workspaceRole);
                } catch (BusinessException ignored) {
                    // not a member of current workspace — leave defaults
                }
            }
        }
        return new AuthDtos.MeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole(),
                workspace,
                workspaceRole,
                canMutate
        );
    }

    @Transactional
    public AuthDtos.LoginResponse switchWorkspace(NimbusPrincipal principal, UUID workspaceId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        workspaceBootstrapService.requireMember(workspaceId, user.getId());
        user.switchWorkspace(workspaceId);
        userRepository.save(user);
        AuthDtos.LoginResponse tokens = issueTokens(user);
        auditService.recordSuccess(
                principal,
                AuditAction.SWITCH_WORKSPACE,
                "WORKSPACE",
                workspaceId,
                null,
                workspaceId,
                "워크스페이스 전환"
        );
        return tokens;
    }

    public AuthDtos.TokenValidateResponse validate(NimbusPrincipal principal) {
        return new AuthDtos.TokenValidateResponse(true, jwtTokenService.getAccessTokenTtlSeconds());
    }

    @Transactional(readOnly = true)
    public AuthDtos.PermissionsResponse permissions(NimbusPrincipal principal) {
        List<String> permissions = new ArrayList<>();
        permissions.add("PROJECT_READ");
        permissions.add("SERVICE_READ");
        permissions.add("AUDIT_READ");

        WorkspaceRole workspaceRole = null;
        boolean canMutate = false;
        UUID workspaceId = principal.workspaceId();
        if (workspaceId != null) {
            try {
                WorkspaceMember member = workspaceBootstrapService.requireMember(workspaceId, principal.userId());
                workspaceRole = member.getRole();
                canMutate = WorkspacePermissionService.CAN_MUTATE.contains(workspaceRole);
            } catch (BusinessException ignored) {
                // not a member
            }
        }

        if (canMutate) {
            permissions.add("WORKSPACE_MUTATE");
            permissions.add("PROJECT_CREATE");
            permissions.add("PROJECT_UPDATE");
            permissions.add("PROJECT_DELETE");
            permissions.add("ENVIRONMENT_MUTATE");
            permissions.add("PROMOTE");
            permissions.add("SECRET_MUTATE");
            permissions.add("SECRET_REVEAL");
            permissions.add("WIZARD_EXECUTE");
            permissions.add("WIZARD_RETRY");
            permissions.add("DEPLOY");
            permissions.add("AI_REVIEW");
        }

        if (workspaceRole == WorkspaceRole.OWNER
                || workspaceRole == WorkspaceRole.ADMIN
                || workspaceRole == WorkspaceRole.PLATFORM_ENGINEER
                || principal.role() == GlobalRole.ADMIN
                || principal.role() == GlobalRole.PLATFORM_ENGINEER) {
            permissions.add("INFRA_MANAGE");
        }
        if (workspaceRole == WorkspaceRole.OWNER
                || workspaceRole == WorkspaceRole.ADMIN
                || principal.role() == GlobalRole.ADMIN) {
            permissions.add("WORKSPACE_ADMIN");
        }
        return new AuthDtos.PermissionsResponse(permissions, workspaceRole, canMutate);
    }

    private void ensureWorkspace(User user) {
        if (user.getCurrentWorkspaceId() != null
                && workspaceRepository.findByIdAndDeletedAtIsNull(user.getCurrentWorkspaceId()).isPresent()) {
            return;
        }
        Workspace workspace = workspaceBootstrapService.createPersonalWorkspace(user);
        user.switchWorkspace(workspace.getId());
    }

    private AuthDtos.LoginResponse issueTokens(User user) {
        NimbusPrincipal principal = new NimbusPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCurrentWorkspaceId()
        );
        String accessToken = jwtTokenService.createAccessToken(principal);
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID();
        tokenStore.put(
                "refresh:" + refreshToken,
                user.getId().toString(),
                Duration.ofSeconds(jwtTokenService.getRefreshTokenTtlSeconds())
        );
        return new AuthDtos.LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenTtlSeconds(),
                new AuthDtos.UserSummary(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getAvatarUrl(),
                        user.getRole(),
                        user.getCurrentWorkspaceId()
                )
        );
    }

    private String exchangeGithubCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", githubProperties.getClientId());
        form.add("client_secret", githubProperties.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", githubProperties.getRedirectUri());

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
                throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED, node.path("error_description").asText("OAuth error"));
            }
            return node.path("access_token").asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_FAILED);
        }
    }

    private JsonNode fetchGithubUser(String accessToken) throws Exception {
        String body = restClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(String.class);
        return objectMapper.readTree(body);
    }

    private String fetchPrimaryEmail(String accessToken) {
        try {
            String body = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(String.class);
            JsonNode arr = objectMapper.readTree(body);
            for (JsonNode node : arr) {
                if (node.path("primary").asBoolean(false) && node.path("verified").asBoolean(false)) {
                    return node.path("email").asText();
                }
            }
            if (arr.isArray() && !arr.isEmpty()) {
                return arr.get(0).path("email").asText(null);
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch GitHub emails: {}", ex.getMessage());
        }
        return null;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
