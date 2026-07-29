package io.nimbus.platform.workspace.repository;

import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    Optional<WorkspaceMember> findByWorkspaceIdAndUserIdAndDeletedAtIsNull(UUID workspaceId, UUID userId);

    List<WorkspaceMember> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<WorkspaceMember> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    long countByWorkspaceIdAndRoleAndDeletedAtIsNull(UUID workspaceId, WorkspaceRole role);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    boolean existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(UUID workspaceId, UUID userId);

    long countByTeamIdAndDeletedAtIsNull(UUID teamId);
}
