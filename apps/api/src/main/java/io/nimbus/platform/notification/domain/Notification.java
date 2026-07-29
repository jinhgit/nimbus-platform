package io.nimbus.platform.notification.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    /** null = workspace-wide */
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type = NotificationType.SYSTEM;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(length = 300)
    private String href;

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public static Notification create(
            UUID workspaceId,
            UUID userId,
            NotificationType type,
            String title,
            String body,
            String href,
            String sourceType,
            UUID sourceId
    ) {
        Notification n = new Notification();
        n.workspaceId = workspaceId;
        n.userId = userId;
        n.type = type != null ? type : NotificationType.SYSTEM;
        n.title = title;
        n.body = body;
        n.href = href;
        n.sourceType = sourceType;
        n.sourceId = sourceId;
        return n;
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getHref() {
        return href;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
