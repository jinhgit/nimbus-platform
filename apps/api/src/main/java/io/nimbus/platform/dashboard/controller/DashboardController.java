package io.nimbus.platform.dashboard.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.dashboard.dto.DashboardDtos;
import io.nimbus.platform.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardDtos.OverviewResponse> overview(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(dashboardService.overview(principal, workspaceId));
    }
}
