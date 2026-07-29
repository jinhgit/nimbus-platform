package io.nimbus.platform.k8s.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.k8s.dto.K8sDtos;
import io.nimbus.platform.k8s.service.K8sDeployService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/k8s")
public class KubernetesController {

    private final K8sDeployService k8sDeployService;

    public KubernetesController(K8sDeployService k8sDeployService) {
        this.k8sDeployService = k8sDeployService;
    }

    @GetMapping("/cluster")
    public ApiResponse<K8sDtos.ClusterStatusResponse> cluster() {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(k8sDeployService.clusterStatus());
    }

    @PostMapping("/cluster/refresh")
    public ApiResponse<K8sDtos.ClusterStatusResponse> refresh() {
        SecurityUtils.requirePrincipal();
        k8sDeployService.refreshConnection();
        return ApiResponse.ok(k8sDeployService.clusterStatus());
    }

    @GetMapping("/deployments")
    public ApiResponse<List<K8sDtos.DeploymentResponse>> deployments(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        return ApiResponse.ok(k8sDeployService.listDeployments(ws));
    }

    @GetMapping("/deployments/by-service/{serviceId}")
    public ApiResponse<K8sDtos.DeploymentResponse> byService(@PathVariable UUID serviceId) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(k8sDeployService.getByService(serviceId));
    }
}
