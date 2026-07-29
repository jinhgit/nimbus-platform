package io.nimbus.platform.notification.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.notification.dto.NotificationDtos;
import io.nimbus.platform.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDtos.NotificationResponse>> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(defaultValue = "30") int limit
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(notificationService.list(principal, workspaceId, limit));
    }

    @GetMapping("/unread-count")
    public ApiResponse<NotificationDtos.UnreadCountResponse> unreadCount(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(notificationService.unreadCount(principal, workspaceId));
    }

    @PostMapping("/sync")
    public ApiResponse<NotificationDtos.SyncResponse> sync(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(notificationService.sync(principal, workspaceId));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationDtos.NotificationResponse> markRead(
            @PathVariable UUID notificationId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(notificationService.markRead(principal, notificationId));
    }

    @PostMapping("/read-all")
    public ApiResponse<NotificationDtos.UnreadCountResponse> markAllRead(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(notificationService.markAllRead(principal, workspaceId));
    }
}
