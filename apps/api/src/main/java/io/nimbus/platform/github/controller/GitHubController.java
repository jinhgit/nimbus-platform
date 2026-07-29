package io.nimbus.platform.github.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.dto.GitHubDtos;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.github.service.GitHubProvisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/connect")
    public ApiResponse<GitHubDtos.ConnectionResponse> connect(
            @Valid @RequestBody GitHubDtos.ConnectRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(connectionService.connect(principal, request.personalAccessToken()));
    }

    @GetMapping("/connection")
    public ApiResponse<GitHubDtos.ConnectionResponse> connection() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return connectionService.getConnection(principal)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        boolean connected = connectionService.getConnection(principal).isPresent();
        return ApiResponse.ok(Map.of(
                "connected", connected,
                "provider", "GitHub"
        ));
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
