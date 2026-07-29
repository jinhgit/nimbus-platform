package io.nimbus.platform.notification.dto;

import io.nimbus.platform.notification.domain.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID id,
            UUID workspaceId,
            NotificationType type,
            String title,
            String body,
            String href,
            String sourceType,
            UUID sourceId,
            boolean unread,
            Instant createdAt,
            Instant readAt
    ) {
    }

    public record UnreadCountResponse(long unread) {
    }

    public record SyncResponse(int created, int scanned, List<NotificationResponse> items) {
    }
}
