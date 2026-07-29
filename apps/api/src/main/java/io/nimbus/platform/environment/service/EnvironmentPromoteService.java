package io.nimbus.platform.environment.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.domain.DeploymentStrategy;
import io.nimbus.platform.environment.domain.EnvironmentStatus;
import io.nimbus.platform.environment.domain.PromotionRecord;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.dto.ConfigDtos;
import io.nimbus.platform.deployment.domain.DeploymentStatus;
import io.nimbus.platform.deployment.domain.DeploymentTrigger;
import io.nimbus.platform.deployment.service.DeploymentService;
import io.nimbus.platform.environment.repository.PromotionRecordRepository;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sprint B: DEV → STAGE → PRODUCTION 상태 전이 + config 복사 + Audit.
 * GitOps 실 PR/Sync 는 후속.
 */
@Service
public class EnvironmentPromoteService {

    private final ServiceEnvironmentRepository environmentRepository;
    private final AppServiceRepository appServiceRepository;
    private final PromotionRecordRepository promotionRecordRepository;
    private final EnvironmentConfigService configService;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditService auditService;
    private final DeploymentService deploymentService;

    public EnvironmentPromoteService(
            ServiceEnvironmentRepository environmentRepository,
            AppServiceRepository appServiceRepository,
            PromotionRecordRepository promotionRecordRepository,
            EnvironmentConfigService configService,
            WorkspaceBootstrapService workspaceBootstrapService,
            WorkspacePermissionService workspacePermissionService,
            AuditService auditService,
            DeploymentService deploymentService
    ) {
        this.environmentRepository = environmentRepository;
        this.appServiceRepository = appServiceRepository;
        this.promotionRecordRepository = promotionRecordRepository;
        this.configService = configService;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.workspacePermissionService = workspacePermissionService;
        this.auditService = auditService;
        this.deploymentService = deploymentService;
    }

    @Transactional
    public ConfigDtos.PromoteResponse promote(
            NimbusPrincipal principal,
            UUID sourceEnvironmentId,
            ConfigDtos.PromoteRequest request
    ) {
        ServiceEnvironment source = environmentRepository.findByIdAndDeletedAtIsNull(sourceEnvironmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
        workspacePermissionService.requireMutator(source.getWorkspaceId(), principal.userId());

        if (source.getStatus() == EnvironmentStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_ARCHIVED);
        }
        if (source.getStatus() != EnvironmentStatus.READY
                && source.getStatus() != EnvironmentStatus.FAILED) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_INVALID_STATE,
                    "Source must be READY (or FAILED) to promote");
        }

        EnvironmentType targetType = request.target();
        validatePromotionPath(source.getType(), targetType);

        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(source.getServiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

        source.markDeploying();
        environmentRepository.save(source);

        ServiceEnvironment target = environmentRepository
                .findByServiceIdAndTypeAndDeletedAtIsNull(source.getServiceId(), targetType)
                .orElseGet(() -> createTargetFromSource(source, service, targetType, principal.userId()));

        if (target.getStatus() == EnvironmentStatus.ARCHIVED) {
            target.restoreFromArchive();
        }
        target.markDeploying();
        environmentRepository.save(target);

        int vars = configService.copyVariables(source.getId(), target, principal.userId());
        int secrets = configService.copySecrets(source.getId(), target, principal.userId());

        // 정책/스펙 일부 승격 (replica 등 상향 최소 유지)
        target.update(
                target.getDomain() != null ? target.getDomain() : source.getDomain(),
                source.getDeploymentStrategy(),
                Math.max(
                        source.getReplicaCount() != null ? source.getReplicaCount() : 1,
                        target.getReplicaCount() != null ? target.getReplicaCount() : 1
                ),
                source.getCpu(),
                source.getMemory(),
                source.getHpaEnabled(),
                target.getGitOpsBranch()
        );
        target.markReady();
        target.applyHealth(
                source.getHealthStatus(),
                "Promoted from " + source.getType() + " (config copy)"
        );
        environmentRepository.save(target);

        source.markReady();
        environmentRepository.save(source);

        String message = "Promoted " + source.getType() + " → " + targetType
                + " (variables=" + vars + ", secrets=" + secrets + ")";
        PromotionRecord record = promotionRecordRepository.save(PromotionRecord.create(
                source.getServiceId(),
                source.getWorkspaceId(),
                source.getId(),
                target.getId(),
                source.getType(),
                targetType,
                vars,
                secrets,
                message,
                principal.userId()
        ));

        auditService.recordSuccess(
                principal,
                AuditAction.PROMOTE_ENVIRONMENT,
                "ENVIRONMENT",
                target.getId(),
                service.getName() + "/" + source.getType() + "→" + targetType,
                source.getWorkspaceId(),
                message
        );

        deploymentService.record(
                service.getId(),
                target.getId(),
                service.getWorkspaceId(),
                service.getProjectId(),
                targetType,
                DeploymentStatus.SUCCESS,
                DeploymentTrigger.ENVIRONMENT_PROMOTE,
                DeploymentService.versionNow(),
                service.getK8sDeployment(),
                target.getNamespaceName(),
                message,
                record.getId(),
                null,
                null,
                principal.userId()
        );

        return toPromoteResponse(record);
    }

    @Transactional(readOnly = true)
    public ConfigDtos.PromotionListResponse listPromotions(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
        List<ConfigDtos.PromoteResponse> items = promotionRecordRepository
                .findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(serviceId)
                .stream()
                .map(this::toPromoteResponse)
                .toList();
        return new ConfigDtos.PromotionListResponse(items);
    }

    private ServiceEnvironment createTargetFromSource(
            ServiceEnvironment source,
            AppService service,
            EnvironmentType targetType,
            UUID actorId
    ) {
        String ns = EnvironmentService.defaultNamespace(service.getName(), targetType);
        int replicas = source.getReplicaCount() != null ? source.getReplicaCount() : 1;
        if (targetType == EnvironmentType.PRODUCTION && replicas < 2) {
            replicas = 2;
        }
        ServiceEnvironment env = ServiceEnvironment.create(
                source.getServiceId(),
                source.getProjectId(),
                source.getWorkspaceId(),
                targetType,
                ns,
                source.getDomain(),
                source.getClusterLabel(),
                source.getDeploymentStrategy() != null
                        ? source.getDeploymentStrategy()
                        : DeploymentStrategy.ROLLING,
                replicas,
                source.getCpu(),
                source.getMemory(),
                source.getHpaEnabled(),
                ServiceEnvironment.defaultBranch(targetType),
                actorId
        );
        return environmentRepository.save(env);
    }

    /**
     * DEV → STAGE, STAGE → PRODUCTION only (skip-level 금지).
     */
    static void validatePromotionPath(EnvironmentType source, EnvironmentType target) {
        if (source == target) {
            throw new BusinessException(ErrorCode.PROMOTE_INVALID_PATH, "Target must differ from source");
        }
        boolean ok = (source == EnvironmentType.DEV && target == EnvironmentType.STAGE)
                || (source == EnvironmentType.STAGE && target == EnvironmentType.PRODUCTION);
        if (!ok) {
            throw new BusinessException(
                    ErrorCode.PROMOTE_INVALID_PATH,
                    "Allowed path: DEV→STAGE or STAGE→PRODUCTION (got " + source + "→" + target + ")"
            );
        }
    }

    private ConfigDtos.PromoteResponse toPromoteResponse(PromotionRecord r) {
        return new ConfigDtos.PromoteResponse(
                r.getId(),
                r.getStatus(),
                r.getSourceEnvironmentId(),
                r.getTargetEnvironmentId(),
                r.getSourceType(),
                r.getTargetType(),
                r.getVariablesCopied() != null ? r.getVariablesCopied() : 0,
                r.getSecretsCopied() != null ? r.getSecretsCopied() : 0,
                r.getMessage(),
                r.getFinishedAt() != null ? r.getFinishedAt() : r.getCreatedAt()
        );
    }
}
