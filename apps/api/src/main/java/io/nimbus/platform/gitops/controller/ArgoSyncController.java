package io.nimbus.platform.gitops.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.gitops.dto.ArgoDtos;
import io.nimbus.platform.gitops.service.ArgoSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ArgoSyncController {

    private final ArgoSyncService argoSyncService;

    public ArgoSyncController(ArgoSyncService argoSyncService) {
        this.argoSyncService = argoSyncService;
    }

    @GetMapping("/services/{serviceId}/argo-sync")
    public ApiResponse<ArgoDtos.ArgoSyncResponse> status(@PathVariable UUID serviceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(argoSyncService.status(principal, serviceId));
    }
}
