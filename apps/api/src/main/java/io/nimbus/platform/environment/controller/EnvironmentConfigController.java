package io.nimbus.platform.environment.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.environment.dto.ConfigDtos;
import io.nimbus.platform.environment.service.EnvironmentConfigService;
import io.nimbus.platform.environment.service.EnvironmentPromoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EnvironmentConfigController {

    private final EnvironmentConfigService configService;
    private final EnvironmentPromoteService promoteService;
    private final io.nimbus.platform.environment.service.GitHubSecretSyncService secretSyncService;

    public EnvironmentConfigController(
            EnvironmentConfigService configService,
            EnvironmentPromoteService promoteService,
            io.nimbus.platform.environment.service.GitHubSecretSyncService secretSyncService
    ) {
        this.configService = configService;
        this.promoteService = promoteService;
        this.secretSyncService = secretSyncService;
    }

    // ── Variables ──────────────────────────────────────────

    @PostMapping("/environments/{environmentId}/variables")
    public ApiResponse<ConfigDtos.VariableResponse> createVariable(
            @PathVariable UUID environmentId,
            @Valid @RequestBody ConfigDtos.UpsertVariableRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.createVariable(principal, environmentId, request));
    }

    @GetMapping("/environments/{environmentId}/variables")
    public ApiResponse<List<ConfigDtos.VariableResponse>> listVariables(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.listVariables(principal, environmentId));
    }

    @PatchMapping("/variables/{variableId}")
    public ApiResponse<ConfigDtos.VariableResponse> updateVariable(
            @PathVariable UUID variableId,
            @Valid @RequestBody ConfigDtos.UpdateVariableRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.updateVariable(principal, variableId, request));
    }

    @DeleteMapping("/variables/{variableId}")
    public ApiResponse<Void> deleteVariable(@PathVariable UUID variableId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        configService.deleteVariable(principal, variableId);
        return ApiResponse.ok(null);
    }

    // ── Secrets ────────────────────────────────────────────

    @PostMapping("/environments/{environmentId}/secrets")
    public ApiResponse<ConfigDtos.SecretResponse> createSecret(
            @PathVariable UUID environmentId,
            @Valid @RequestBody ConfigDtos.UpsertSecretRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.createSecret(principal, environmentId, request));
    }

    @GetMapping("/environments/{environmentId}/secrets")
    public ApiResponse<List<ConfigDtos.SecretResponse>> listSecrets(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.listSecrets(principal, environmentId));
    }

    @PatchMapping("/secrets/{secretId}")
    public ApiResponse<ConfigDtos.SecretResponse> updateSecret(
            @PathVariable UUID secretId,
            @Valid @RequestBody ConfigDtos.UpdateSecretRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.updateSecret(principal, secretId, request));
    }

    @DeleteMapping("/secrets/{secretId}")
    public ApiResponse<Void> deleteSecret(@PathVariable UUID secretId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        configService.deleteSecret(principal, secretId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/secrets/{secretId}/reveal")
    public ApiResponse<ConfigDtos.SecretRevealResponse> revealSecret(@PathVariable UUID secretId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.revealSecret(principal, secretId));
    }

    @PostMapping("/secrets/{secretId}/rotate")
    public ApiResponse<ConfigDtos.RotateSecretResponse> rotateSecret(
            @PathVariable UUID secretId,
            @RequestBody(required = false) ConfigDtos.RotateSecretRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(configService.rotateSecret(
                principal,
                secretId,
                request != null ? request : new ConfigDtos.RotateSecretRequest(null, true)
        ));
    }

    @PostMapping("/environments/{environmentId}/secrets/sync-github")
    public ApiResponse<ConfigDtos.SecretSyncResponse> syncGitHubSecrets(
            @PathVariable UUID environmentId,
            @RequestBody(required = false) ConfigDtos.SecretSyncRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(secretSyncService.sync(principal, environmentId, request));
    }

    // ── Promote ────────────────────────────────────────────

    @PostMapping("/environments/{environmentId}/promote")
    public ApiResponse<ConfigDtos.PromoteResponse> promote(
            @PathVariable UUID environmentId,
            @Valid @RequestBody ConfigDtos.PromoteRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(promoteService.promote(principal, environmentId, request));
    }

    @GetMapping("/services/{serviceId}/promotions")
    public ApiResponse<ConfigDtos.PromotionListResponse> listPromotions(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(promoteService.listPromotions(principal, serviceId));
    }
}
