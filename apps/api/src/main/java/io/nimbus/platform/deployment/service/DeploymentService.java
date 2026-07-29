package io.nimbus.platform.deployment.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.deployment.domain.DeploymentStatus;
import io.nimbus.platform.deployment.domain.DeploymentTrigger;
import io.nimbus.platform.deployment.domain.ServiceDeployment;
import io.nimbus.platform.deployment.dto.DeploymentDtos;
import io.nimbus.platform.deployment.repository.ServiceDeploymentRepository;
import io.nimbus.platform.environment.domain.PromotionRecord;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.repository.PromotionRecordRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final ServiceDeploymentRepository deploymentRepository;
    private final AppServiceRepository appServiceRepository;
    private final PromotionRecordRepository promotionRecordRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final AuditService auditService;

    public DeploymentService(
            ServiceDeploymentRepository deploymentRepository,
            AppServiceRepository appServiceRepository,
            PromotionRecordRepository promotionRecordRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            AuditService auditService
    ) {
        this.deploymentRepository = deploymentRepository;
        this.appServiceRepository = appServiceRepository;
        this.promotionRecordRepository = promotionRecordRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.auditService = auditService;
    }

    /**
     * 시스템/내부 호출 — principal 없이 기록.
     */
    @Transactional
    public ServiceDeployment record(
            UUID serviceId,
            UUID environmentId,
            UUID workspaceId,
            UUID projectId,
            EnvironmentType environmentType,
            DeploymentStatus status,
            DeploymentTrigger trigger,
            String versionLabel,
            String imageTag,
            String namespaceName,
            String message,
            UUID promotionId,
            UUID wizardId,
            UUID pipelineId,
            UUID triggeredBy
    ) {
        ServiceDeployment saved = deploymentRepository.save(ServiceDeployment.create(
                serviceId, environmentId, workspaceId, projectId, environmentType,
                status, trigger, versionLabel, imageTag, namespaceName, message,
                promotionId, wizardId, pipelineId, triggeredBy
        ));
        auditService.recordRaw(
                triggeredBy,
                null,
                null,
                AuditAction.RECORD_DEPLOYMENT,
                "DEPLOYMENT",
                saved.getId(),
                versionLabel != null ? versionLabel : trigger.name(),
                workspaceId,
                null,
                message != null ? message : trigger.name()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public DeploymentDtos.DeploymentListResponse listByService(NimbusPrincipal principal, UUID serviceId) {
        AppService service = requireService(principal, serviceId);
        List<DeploymentDtos.DeploymentResponse> items = deploymentRepository
                .findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(serviceId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new DeploymentDtos.DeploymentListResponse(service.getId(), items, items.size());
    }

    @Transactional(readOnly = true)
    public DeploymentDtos.TimelineResponse timeline(NimbusPrincipal principal, UUID serviceId) {
        AppService service = requireService(principal, serviceId);
        List<DeploymentDtos.TimelineItem> items = new ArrayList<>();

        for (ServiceDeployment d : deploymentRepository.findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(serviceId)) {
            items.add(new DeploymentDtos.TimelineItem(
                    "DEPLOYMENT",
                    d.getId(),
                    d.getFinishedAt() != null ? d.getFinishedAt() : d.getCreatedAt(),
                    d.getTrigger().name() + " · " + (d.getEnvironmentType() != null ? d.getEnvironmentType().name() : "—"),
                    d.getMessage() != null ? d.getMessage() : d.getVersionLabel(),
                    d.getStatus().name()
            ));
        }
        for (PromotionRecord p : promotionRecordRepository.findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(serviceId)) {
            items.add(new DeploymentDtos.TimelineItem(
                    "PROMOTION",
                    p.getId(),
                    p.getFinishedAt() != null ? p.getFinishedAt() : p.getCreatedAt(),
                    p.getSourceType() + " → " + p.getTargetType(),
                    p.getMessage(),
                    p.getStatus().name()
            ));
        }

        items.sort(Comparator.comparing(DeploymentDtos.TimelineItem::at, Comparator.nullsLast(Comparator.reverseOrder())));
        if (items.size() > 50) {
            items = items.subList(0, 50);
        }
        return new DeploymentDtos.TimelineResponse(service.getId(), items);
    }

    private AppService requireService(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
        return service;
    }

    private DeploymentDtos.DeploymentResponse toResponse(ServiceDeployment d) {
        return new DeploymentDtos.DeploymentResponse(
                d.getId(),
                d.getServiceId(),
                d.getEnvironmentId(),
                d.getEnvironmentType(),
                d.getStatus(),
                d.getTrigger(),
                d.getVersionLabel(),
                d.getImageTag(),
                d.getNamespaceName(),
                d.getMessage(),
                d.getPromotionId(),
                d.getWizardId(),
                d.getPipelineId(),
                d.getTriggeredBy(),
                d.getCreatedAt(),
                d.getFinishedAt()
        );
    }

    public static String versionNow() {
        return "v" + Instant.now().getEpochSecond();
    }
}
