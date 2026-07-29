package io.nimbus.platform.audit.controller;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.dto.AuditDtos;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<AuditDtos.AuditListResponse> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Integer limit
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(auditService.list(principal, workspaceId, actorId, action, resourceType, limit));
    }
}
