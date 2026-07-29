package io.nimbus.platform.deployment.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.deployment.dto.DeploymentDtos;
import io.nimbus.platform.deployment.service.DeploymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping("/services/{serviceId}/deployments")
    public ApiResponse<DeploymentDtos.DeploymentListResponse> list(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(deploymentService.listByService(principal, serviceId));
    }

    @GetMapping("/services/{serviceId}/timeline")
    public ApiResponse<DeploymentDtos.TimelineResponse> timeline(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(deploymentService.timeline(principal, serviceId));
    }
}
