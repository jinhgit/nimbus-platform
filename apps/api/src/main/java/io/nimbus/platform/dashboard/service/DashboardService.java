package io.nimbus.platform.dashboard.service;

import io.nimbus.platform.audit.domain.AuditLog;
import io.nimbus.platform.audit.repository.AuditLogRepository;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.dashboard.dto.DashboardDtos;
import io.nimbus.platform.environment.domain.PromotionRecord;
import io.nimbus.platform.environment.repository.PromotionRecordRepository;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.domain.SagaStatus;
import io.nimbus.platform.provision.repository.ProvisionSagaRepository;
import io.nimbus.platform.serviceapp.domain.ServiceStatus;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DashboardService {

    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final ProjectRepository projectRepository;
    private final AppServiceRepository appServiceRepository;
    private final ServiceEnvironmentRepository environmentRepository;
    private final PromotionRecordRepository promotionRecordRepository;
    private final ProvisionSagaRepository sagaRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(
            WorkspaceBootstrapService workspaceBootstrapService,
            ProjectRepository projectRepository,
            AppServiceRepository appServiceRepository,
            ServiceEnvironmentRepository environmentRepository,
            PromotionRecordRepository promotionRecordRepository,
            ProvisionSagaRepository sagaRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.projectRepository = projectRepository;
        this.appServiceRepository = appServiceRepository;
        this.environmentRepository = environmentRepository;
        this.promotionRecordRepository = promotionRecordRepository;
        this.sagaRepository = sagaRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.OverviewResponse overview(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        WorkspaceMember member = workspaceBootstrapService.requireMember(ws, principal.userId());
        WorkspaceRole role = member.getRole();
        boolean canMutate = WorkspacePermissionService.CAN_MUTATE.contains(role);

        long projects = projectRepository.countByWorkspaceIdAndDeletedAtIsNull(ws);
        long services = appServiceRepository.countByWorkspaceIdAndDeletedAtIsNull(ws);
        long environments = environmentRepository.countByWorkspaceIdAndDeletedAtIsNull(ws);
        long ready = appServiceRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, ServiceStatus.READY);
        long failedSagas = sagaRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, SagaStatus.FAILED)
                + sagaRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, SagaStatus.ROLLED_BACK);
        long audits = auditLogRepository.countByWorkspaceId(ws);

        List<DashboardDtos.PromoteItem> promotes = promotionRecordRepository
                .findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(ws, PageRequest.of(0, 5))
                .stream()
                .map(this::toPromote)
                .toList();

        List<DashboardDtos.SagaItem> sagas = sagaRepository
                .findFailedByWorkspace(ws, PageRequest.of(0, 5))
                .stream()
                .map(this::toSaga)
                .toList();

        List<DashboardDtos.AuditItem> auditItems = auditLogRepository
                .search(ws, null, null, null, PageRequest.of(0, 8))
                .stream()
                .map(this::toAudit)
                .toList();

        return new DashboardDtos.OverviewResponse(
                ws,
                role,
                canMutate,
                new DashboardDtos.Counts(projects, services, environments, ready, failedSagas, audits),
                promotes,
                sagas,
                auditItems
        );
    }

    private DashboardDtos.PromoteItem toPromote(PromotionRecord p) {
        return new DashboardDtos.PromoteItem(
                p.getId(),
                p.getServiceId(),
                p.getSourceType(),
                p.getTargetType(),
                p.getStatus(),
                p.getMessage(),
                p.getFinishedAt() != null ? p.getFinishedAt() : p.getCreatedAt()
        );
    }

    private DashboardDtos.SagaItem toSaga(ProvisionSaga s) {
        return new DashboardDtos.SagaItem(
                s.getId(),
                s.getWizardId(),
                s.getAttempt(),
                s.getStatus(),
                s.getFailureReason(),
                s.getFinishedAt() != null ? s.getFinishedAt() : s.getCreatedAt()
        );
    }

    private DashboardDtos.AuditItem toAudit(AuditLog a) {
        return new DashboardDtos.AuditItem(
                a.getId(),
                a.getAction(),
                a.getResourceType(),
                a.getResourceName(),
                a.getActorName(),
                a.getCreatedAt()
        );
    }
}
