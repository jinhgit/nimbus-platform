package io.nimbus.platform.environment.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.environment.dto.EnvironmentDtos;
import io.nimbus.platform.environment.service.EnvironmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @PostMapping("/services/{serviceId}/environments")
    public ApiResponse<EnvironmentDtos.EnvironmentResponse> create(
            @PathVariable UUID serviceId,
            @Valid @RequestBody EnvironmentDtos.CreateEnvironmentRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.create(principal, serviceId, request));
    }

    @GetMapping("/services/{serviceId}/environments")
    public ApiResponse<EnvironmentDtos.EnvironmentListResponse> list(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.list(principal, serviceId));
    }

    @GetMapping("/environments/{environmentId}")
    public ApiResponse<EnvironmentDtos.EnvironmentResponse> get(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.get(principal, environmentId));
    }

    @PatchMapping("/environments/{environmentId}")
    public ApiResponse<EnvironmentDtos.EnvironmentResponse> update(
            @PathVariable UUID environmentId,
            @Valid @RequestBody EnvironmentDtos.UpdateEnvironmentRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.update(principal, environmentId, request));
    }

    @DeleteMapping("/environments/{environmentId}")
    public ApiResponse<Void> delete(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        environmentService.delete(principal, environmentId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/environments/{environmentId}/archive")
    public ApiResponse<EnvironmentDtos.EnvironmentResponse> archive(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.archive(principal, environmentId));
    }

    @PostMapping("/environments/{environmentId}/restore")
    public ApiResponse<EnvironmentDtos.EnvironmentResponse> restore(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.restore(principal, environmentId));
    }

    @GetMapping("/environments/{environmentId}/health")
    public ApiResponse<EnvironmentDtos.EnvironmentHealthResponse> health(@PathVariable UUID environmentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(environmentService.health(principal, environmentId));
    }
}
