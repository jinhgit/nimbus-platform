package io.nimbus.platform.notification.repository;

import io.nimbus.platform.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.workspaceId = :workspaceId
              AND n.deletedAt IS NULL
              AND (n.userId IS NULL OR n.userId = :userId)
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findForUser(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.workspaceId = :workspaceId
              AND n.deletedAt IS NULL
              AND n.readAt IS NULL
              AND (n.userId IS NULL OR n.userId = :userId)
            """)
    long countUnread(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);

    boolean existsByWorkspaceIdAndSourceTypeAndSourceIdAndDeletedAtIsNull(
            UUID workspaceId, String sourceType, UUID sourceId
    );
}
