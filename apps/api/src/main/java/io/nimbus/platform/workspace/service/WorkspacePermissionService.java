package io.nimbus.platform.workspace.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * 일관된 Workspace 역할 검사 (Sprint C).
 */
@Service
public class WorkspacePermissionService {

    public static final Set<WorkspaceRole> CAN_MUTATE = EnumSet.of(
            WorkspaceRole.OWNER,
            WorkspaceRole.ADMIN,
            WorkspaceRole.PLATFORM_ENGINEER,
            WorkspaceRole.DEVELOPER
    );

    public static final Set<WorkspaceRole> CAN_ADMIN = EnumSet.of(
            WorkspaceRole.OWNER,
            WorkspaceRole.ADMIN,
            WorkspaceRole.PLATFORM_ENGINEER
    );

    private final WorkspaceBootstrapService workspaceBootstrapService;

    public WorkspacePermissionService(WorkspaceBootstrapService workspaceBootstrapService) {
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireMutator(UUID workspaceId, UUID userId) {
        return requireAny(workspaceId, userId, CAN_MUTATE);
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireAdminish(UUID workspaceId, UUID userId) {
        return requireAny(workspaceId, userId, CAN_ADMIN);
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireAny(UUID workspaceId, UUID userId, Set<WorkspaceRole> allowed) {
        WorkspaceMember member = workspaceBootstrapService.requireMember(workspaceId, userId);
        if (!allowed.contains(member.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Insufficient workspace role: " + member.getRole());
        }
        return member;
    }
}
