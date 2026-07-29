package io.nimbus.platform.github.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.dto.GitHubDtos;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.github.service.GitHubProvisionService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubController {

    private final GitHubConnectionService connectionService;
    private final GitHubProvisionService provisionService;

    public GitHubController(
            GitHubConnectionService connectionService,
            GitHubProvisionService provisionService
    ) {
        this.connectionService = connectionService;
        this.provisionService = provisionService;
    }

    /** OAuth App 설정 여부 (UI 표시용) */
    @GetMapping("/oauth/config")
    public ApiResponse<GitHubDtos.OAuthConfigResponse> oauthConfig() {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.oauthConfig());
    }

    /**
     * SCM OAuth 시작 — authorizeUrl 반환.
     * 프론트에서 window.location = authorizeUrl.
     */
    @GetMapping("/oauth/authorize")
    public ApiResponse<GitHubDtos.OAuthAuthorizeResponse> oauthAuthorize() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.startOAuth(principal));
    }

    /**
     * GitHub OAuth callback (public).
     * state 에 userId 가 있으므로 인증 헤더 불필요.
     */
    @GetMapping("/oauth/callback")
    public void oauthCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        String redirect = connectionService.handleOAuthCallback(code, state);
        response.sendRedirect(redirect);
    }

    /** PAT 수동 연결 (보조) */
    @PostMapping("/connect")
    public ApiResponse<GitHubDtos.ConnectionResponse> connect(
            @Valid @RequestBody GitHubDtos.ConnectRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.connectPat(principal, request.personalAccessToken()));
    }

    @GetMapping("/connection")
    public ApiResponse<GitHubDtos.ConnectionResponse> connection() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return connectionService.getConnection(principal)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));
    }

    @GetMapping("/status")
    public ApiResponse<GitHubDtos.StatusResponse> status() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.status(principal));
    }

    @DeleteMapping("/connection")
    public ApiResponse<Void> disconnect() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        connectionService.disconnect(principal);
        return ApiResponse.ok(null);
    }

    @GetMapping("/health")
    public ApiResponse<GitHubDtos.HealthResponse> health() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.health(principal));
    }

    @GetMapping("/repositories")
    public ApiResponse<List<GitHubDtos.RepositoryResponse>> repositories() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(provisionService.listRepos(principal.userId()));
    }
}
