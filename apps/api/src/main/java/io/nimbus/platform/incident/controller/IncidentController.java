package io.nimbus.platform.incident.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.incident.domain.IncidentStatus;
import io.nimbus.platform.incident.dto.IncidentDtos;
import io.nimbus.platform.incident.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ApiResponse<List<IncidentDtos.IncidentResponse>> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) IncidentStatus status
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.list(principal, workspaceId, status));
    }

    @GetMapping("/counts")
    public ApiResponse<IncidentDtos.CountsResponse> counts(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.counts(principal, workspaceId));
    }

    @GetMapping("/{incidentId}")
    public ApiResponse<IncidentDtos.IncidentResponse> get(@PathVariable UUID incidentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.get(principal, incidentId));
    }

    @PostMapping("/scan")
    public ApiResponse<IncidentDtos.ScanResponse> scan(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.scan(principal, workspaceId));
    }

    @PostMapping("/{incidentId}/acknowledge")
    public ApiResponse<IncidentDtos.IncidentResponse> acknowledge(@PathVariable UUID incidentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.acknowledge(principal, incidentId));
    }

    @PostMapping("/{incidentId}/resolve")
    public ApiResponse<IncidentDtos.IncidentResponse> resolve(@PathVariable UUID incidentId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(incidentService.resolve(principal, incidentId));
    }
}
