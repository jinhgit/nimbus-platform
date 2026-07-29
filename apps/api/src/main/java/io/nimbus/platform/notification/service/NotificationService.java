package io.nimbus.platform.notification.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.incident.domain.Incident;
import io.nimbus.platform.incident.domain.IncidentStatus;
import io.nimbus.platform.incident.repository.IncidentRepository;
import io.nimbus.platform.notification.domain.Notification;
import io.nimbus.platform.notification.domain.NotificationType;
import io.nimbus.platform.notification.dto.NotificationDtos;
import io.nimbus.platform.notification.repository.NotificationRepository;
import io.nimbus.platform.pipeline.domain.BuildPipeline;
import io.nimbus.platform.pipeline.domain.PipelineStatus;
import io.nimbus.platform.pipeline.repository.BuildPipelineRepository;
import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.repository.ProvisionSagaRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final IncidentRepository incidentRepository;
    private final ProvisionSagaRepository sagaRepository;
    private final BuildPipelineRepository pipelineRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public NotificationService(
            NotificationRepository notificationRepository,
            IncidentRepository incidentRepository,
            ProvisionSagaRepository sagaRepository,
            BuildPipelineRepository pipelineRepository,
            WorkspaceBootstrapService workspaceBootstrapService
    ) {
        this.notificationRepository = notificationRepository;
        this.incidentRepository = incidentRepository;
        this.sagaRepository = sagaRepository;
        this.pipelineRepository = pipelineRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> list(
            NimbusPrincipal principal,
            UUID workspaceId,
            int limit
    ) {
        UUID ws = requireWorkspace(principal, workspaceId);
        int size = Math.min(Math.max(limit, 1), 100);
        return notificationRepository
                .findForUser(ws, principal.userId(), PageRequest.of(0, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCountResponse unreadCount(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = requireWorkspace(principal, workspaceId);
        return new NotificationDtos.UnreadCountResponse(
                notificationRepository.countUnread(ws, principal.userId())
        );
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(NimbusPrincipal principal, UUID id) {
        Notification n = notificationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        workspaceBootstrapService.requireMember(n.getWorkspaceId(), principal.userId());
        if (n.getUserId() != null && !n.getUserId().equals(principal.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Not your notification");
        }
        n.markRead();
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public NotificationDtos.UnreadCountResponse markAllRead(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = requireWorkspace(principal, workspaceId);
        List<Notification> items = notificationRepository.findForUser(
                ws, principal.userId(), PageRequest.of(0, 200)
        );
        for (Notification n : items) {
            if (n.isUnread()) {
                n.markRead();
                notificationRepository.save(n);
            }
        }
        return new NotificationDtos.UnreadCountResponse(0);
    }

    /**
     * Deduped sync from open incidents / failed sagas / failed pipelines.
     */
    @Transactional
    public NotificationDtos.SyncResponse sync(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = requireWorkspace(principal, workspaceId);
        List<NotificationDtos.NotificationResponse> created = new ArrayList<>();
        int scanned = 0;

        List<Incident> openIncidents = incidentRepository
                .findByWorkspaceIdAndStatusAndDeletedAtIsNullOrderByOpenedAtDesc(ws, IncidentStatus.OPEN);
        for (Incident inc : openIncidents) {
            scanned++;
            Notification n = createIfAbsent(
                    ws, null, NotificationType.INCIDENT,
                    "Incident: " + inc.getTitle(),
                    inc.getSummary(),
                    "/incidents",
                    "INCIDENT",
                    inc.getId()
            );
            if (n != null) created.add(toResponse(n));
        }

        List<ProvisionSaga> sagas = sagaRepository.findFailedByWorkspace(ws, PageRequest.of(0, 30));
        for (ProvisionSaga saga : sagas) {
            scanned++;
            Notification n = createIfAbsent(
                    ws, null, NotificationType.SAGA,
                    "Saga " + saga.getStatus() + " attempt " + saga.getAttempt(),
                    saga.getFailureReason(),
                    "/wizard",
                    "SAGA",
                    saga.getId()
            );
            if (n != null) created.add(toResponse(n));
        }

        for (BuildPipeline p : pipelineRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(ws)) {
            if (p.getStatus() != PipelineStatus.FAILED) continue;
            scanned++;
            Notification n = createIfAbsent(
                    ws, null, NotificationType.PIPELINE,
                    "Pipeline FAILED: " + p.getServiceName(),
                    p.getCurrentStep(),
                    "/pipelines",
                    "PIPELINE",
                    p.getId()
            );
            if (n != null) created.add(toResponse(n));
        }

        return new NotificationDtos.SyncResponse(created.size(), scanned, created);
    }

    @Transactional
    public void notifyWorkspace(
            UUID workspaceId,
            NotificationType type,
            String title,
            String body,
            String href,
            String sourceType,
            UUID sourceId
    ) {
        createIfAbsent(workspaceId, null, type, title, body, href, sourceType, sourceId);
    }

    private Notification createIfAbsent(
            UUID workspaceId,
            UUID userId,
            NotificationType type,
            String title,
            String body,
            String href,
            String sourceType,
            UUID sourceId
    ) {
        if (sourceType != null && sourceId != null
                && notificationRepository.existsByWorkspaceIdAndSourceTypeAndSourceIdAndDeletedAtIsNull(
                workspaceId, sourceType, sourceId)) {
            return null;
        }
        Notification n = Notification.create(
                workspaceId, userId, type, title,
                body != null && body.length() > 500 ? body.substring(0, 500) : body,
                href, sourceType, sourceId
        );
        return notificationRepository.save(n);
    }

    private UUID requireWorkspace(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(ws, principal.userId());
        return ws;
    }

    private NotificationDtos.NotificationResponse toResponse(Notification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId(),
                n.getWorkspaceId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getHref(),
                n.getSourceType(),
                n.getSourceId(),
                n.isUnread(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
