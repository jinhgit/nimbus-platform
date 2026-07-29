package io.nimbus.platform.observability.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.observability.dto.ObservabilityDtos;
import io.nimbus.platform.observability.service.MonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/links")
    public ApiResponse<ObservabilityDtos.StackLinks> links() {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(monitoringService.links());
    }

    @GetMapping("/overview")
    public ApiResponse<ObservabilityDtos.MonitoringOverview> overview(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        return ApiResponse.ok(monitoringService.overview(ws));
    }

    @GetMapping("/services/{serviceId}/metrics")
    public ApiResponse<ObservabilityDtos.ServiceMetrics> metrics(@PathVariable UUID serviceId) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(monitoringService.serviceMetrics(serviceId));
    }
}
