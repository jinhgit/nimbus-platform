package io.nimbus.platform.serviceapp.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.serviceapp.dto.ServiceDtos;
import io.nimbus.platform.serviceapp.service.AppServiceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ServiceController {

    private final AppServiceQueryService appServiceQueryService;

    public ServiceController(AppServiceQueryService appServiceQueryService) {
        this.appServiceQueryService = appServiceQueryService;
    }

    @GetMapping("/services")
    public ApiResponse<List<ServiceDtos.ServiceResponse>> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID projectId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        if (projectId != null) {
            return ApiResponse.ok(appServiceQueryService.listByProject(principal, projectId));
        }
        return ApiResponse.ok(appServiceQueryService.listByWorkspace(principal, workspaceId));
    }

    @GetMapping("/services/{serviceId}")
    public ApiResponse<ServiceDtos.ServiceResponse> get(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(appServiceQueryService.get(principal, serviceId));
    }

    @GetMapping("/projects/{projectId}/services")
    public ApiResponse<List<ServiceDtos.ServiceResponse>> listByProject(@PathVariable UUID projectId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(appServiceQueryService.listByProject(principal, projectId));
    }
}
