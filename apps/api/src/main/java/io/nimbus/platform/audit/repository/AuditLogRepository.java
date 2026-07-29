package io.nimbus.platform.audit.repository;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.domain.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:workspaceId IS NULL OR a.workspaceId = :workspaceId)
              AND (:actorId IS NULL OR a.actorId = :actorId)
              AND (:action IS NULL OR a.action = :action)
              AND (:resourceType IS NULL OR a.resourceType = :resourceType)
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> search(
            @Param("workspaceId") UUID workspaceId,
            @Param("actorId") UUID actorId,
            @Param("action") AuditAction action,
            @Param("resourceType") String resourceType,
            Pageable pageable
    );

    long countByWorkspaceId(UUID workspaceId);
}
