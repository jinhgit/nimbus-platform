package io.nimbus.platform.audit.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.domain.AuditLog;
import io.nimbus.platform.audit.domain.AuditResult;
import io.nimbus.platform.audit.dto.AuditDtos;
import io.nimbus.platform.audit.repository.AuditLogRepository;
import io.nimbus.platform.audit.support.AuditRequestContext;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AuditLogRepository auditLogRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public AuditService(
            AuditLogRepository auditLogRepository,
            WorkspaceBootstrapService workspaceBootstrapService
    ) {
        this.auditLogRepository = auditLogRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    /**
     * 비즈니스 트랜잭션과 분리해 실패해도 본 트랜잭션을 깨지 않음.
     * (같은 트랜잭션 내 호출 시에도 REQUIRES_NEW 로 커밋 보장)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            NimbusPrincipal principal,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String resourceName,
            UUID workspaceId,
            AuditResult result,
            String message
    ) {
        UUID actorId = principal != null ? principal.userId() : null;
        String email = principal != null ? principal.email() : null;
        String name = principal != null ? principal.name() : null;
        UUID ws = workspaceId != null
                ? workspaceId
                : (principal != null ? principal.workspaceId() : null);
        recordRaw(actorId, email, name, action, resourceType, resourceId, resourceName, ws, result, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            NimbusPrincipal principal,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String resourceName,
            UUID workspaceId,
            String message
    ) {
        record(principal, action, resourceType, resourceId, resourceName, workspaceId, AuditResult.SUCCESS, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRaw(
            UUID actorId,
            String actorEmail,
            String actorName,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String resourceName,
            UUID workspaceId,
            AuditResult result,
            String message
    ) {
        try {
            AuditLog entry = AuditLog.of(
                    actorId,
                    actorEmail,
                    actorName,
                    action,
                    resourceType,
                    resourceId,
                    resourceName,
                    workspaceId,
                    result != null ? result : AuditResult.SUCCESS,
                    message,
                    AuditRequestContext.ipAddress(),
                    AuditRequestContext.userAgent()
            );
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            // 감사 실패가 본 비즈니스 플로우를 막지 않도록 로깅만
            log.warn("Failed to write audit log action={}: {}", action, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuditDtos.AuditListResponse list(
            NimbusPrincipal principal,
            UUID workspaceId,
            UUID actorId,
            AuditAction action,
            String resourceType,
            Integer limit
    ) {
        UUID effectiveWorkspace = workspaceId != null ? workspaceId : principal.workspaceId();
        if (effectiveWorkspace == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(effectiveWorkspace, principal.userId());

        int size = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<AuditLog> rows = auditLogRepository.search(
                effectiveWorkspace,
                actorId,
                action,
                blankToNull(resourceType),
                PageRequest.of(0, size)
        );
        List<AuditDtos.AuditLogResponse> items = rows.stream().map(this::toResponse).toList();
        return new AuditDtos.AuditListResponse(items, items.size(), size);
    }

    private AuditDtos.AuditLogResponse toResponse(AuditLog a) {
        return new AuditDtos.AuditLogResponse(
                a.getId(),
                a.getActorId(),
                a.getActorEmail(),
                a.getActorName(),
                a.getAction(),
                a.getResourceType(),
                a.getResourceId(),
                a.getResourceName(),
                a.getWorkspaceId(),
                a.getResult(),
                a.getMessage(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.getCreatedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
