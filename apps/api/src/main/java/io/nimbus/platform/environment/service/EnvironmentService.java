package io.nimbus.platform.environment.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.domain.DeploymentStrategy;
import io.nimbus.platform.environment.domain.EnvironmentStatus;
import io.nimbus.platform.environment.domain.HealthStatus;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.dto.EnvironmentDtos;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EnvironmentService {

    private static final Comparator<ServiceEnvironment> TYPE_ORDER = Comparator.comparingInt(e -> switch (e.getType()) {
        case DEV -> 0;
        case STAGE -> 1;
        case PRODUCTION -> 2;
    });

    private final ServiceEnvironmentRepository environmentRepository;
    private final AppServiceRepository appServiceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final AuditService auditService;

    public EnvironmentService(
            ServiceEnvironmentRepository environmentRepository,
            AppServiceRepository appServiceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            AuditService auditService
    ) {
        this.environmentRepository = environmentRepository;
        this.appServiceRepository = appServiceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.auditService = auditService;
    }

    /**
     * Wizard / provision 완료 시 기본 Environment 1개 보장.
     */
    @Transactional
    public ServiceEnvironment ensureDefaultForService(AppService service, UUID actorId) {
        EnvironmentType type = service.getEnvironmentType() != null
                ? service.getEnvironmentType()
                : EnvironmentType.DEV;
        return environmentRepository.findByServiceIdAndTypeAndDeletedAtIsNull(service.getId(), type)
                .orElseGet(() -> {
                    String ns = service.getK8sNamespace() != null && !service.getK8sNamespace().isBlank()
                            ? service.getK8sNamespace()
                            : defaultNamespace(service.getName(), type);
                    String cluster = service.getK8sClusterType() != null
                            ? service.getK8sClusterType()
                            : "local";
                    ServiceEnvironment env = ServiceEnvironment.create(
                            service.getId(),
                            service.getProjectId(),
                            service.getWorkspaceId(),
                            type,
                            ns,
                            null,
                            cluster,
                            DeploymentStrategy.ROLLING,
                            service.getReplicaCount(),
                            null,
                            null,
                            service.getHpaEnabled(),
                            ServiceEnvironment.defaultBranch(type),
                            actorId != null ? actorId : service.getOwnerId()
                    );
                    if (service.getK8sStatus() != null && !service.getK8sStatus().isBlank()) {
                        env.applyHealth(mapK8sHealth(service.getK8sStatus()), "Synced from service k8s status");
                    }
                    ServiceEnvironment saved = environmentRepository.save(env);
                    auditService.recordRaw(
                            actorId != null ? actorId : service.getOwnerId(),
                            null,
                            null,
                            AuditAction.CREATE_ENVIRONMENT,
                            "ENVIRONMENT",
                            saved.getId(),
                            service.getName() + "/" + type.name(),
                            service.getWorkspaceId(),
                            null,
                            "Default environment after provision"
                    );
                    return saved;
                });
    }

    @Transactional
    public EnvironmentDtos.EnvironmentResponse create(
            NimbusPrincipal principal,
            UUID serviceId,
            EnvironmentDtos.CreateEnvironmentRequest request
    ) {
        AppService service = requireServiceMember(principal, serviceId);
        if (environmentRepository.existsByServiceIdAndTypeAndDeletedAtIsNull(serviceId, request.type())) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_TYPE_DUPLICATE,
                    request.type() + " environment already exists for this service");
        }

        String ns = request.namespace() != null && !request.namespace().isBlank()
                ? sanitizeNamespace(request.namespace())
                : defaultNamespace(service.getName(), request.type());
        validateNamespace(ns);

        if (request.domain() != null && !request.domain().isBlank()) {
            validateDomain(request.domain());
        }

        ServiceEnvironment env = ServiceEnvironment.create(
                service.getId(),
                service.getProjectId(),
                service.getWorkspaceId(),
                request.type(),
                ns,
                blankToNull(request.domain()),
                request.clusterLabel() != null ? request.clusterLabel() : "local",
                request.deploymentStrategy(),
                request.replicaCount() != null ? request.replicaCount() : service.getReplicaCount(),
                request.cpu(),
                request.memory(),
                request.hpaEnabled() != null ? request.hpaEnabled() : service.getHpaEnabled(),
                request.gitOpsBranch(),
                principal.userId()
        );
        ServiceEnvironment saved = environmentRepository.save(env);
        auditService.recordSuccess(
                principal,
                AuditAction.CREATE_ENVIRONMENT,
                "ENVIRONMENT",
                saved.getId(),
                service.getName() + "/" + saved.getType().name(),
                service.getWorkspaceId(),
                "Environment created"
        );
        return toResponse(saved);
    }

    @Transactional
    public EnvironmentDtos.EnvironmentListResponse list(NimbusPrincipal principal, UUID serviceId) {
        AppService service = requireServiceMember(principal, serviceId);
        // 기존 서비스 마이그레이션: Environment 없으면 Wizard type 기준 1개 자동 보장
        if (environmentRepository.countByServiceIdAndDeletedAtIsNull(serviceId) == 0) {
            ensureDefaultForService(service, principal.userId());
        }
        List<EnvironmentDtos.EnvironmentResponse> items = environmentRepository
                .findByServiceIdAndDeletedAtIsNullOrderByCreatedAtAsc(serviceId)
                .stream()
                .sorted(TYPE_ORDER)
                .map(this::toResponse)
                .toList();
        return new EnvironmentDtos.EnvironmentListResponse(service.getId(), items, items.size());
    }

    @Transactional(readOnly = true)
    public EnvironmentDtos.EnvironmentResponse get(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        return toResponse(env);
    }

    @Transactional
    public EnvironmentDtos.EnvironmentResponse update(
            NimbusPrincipal principal,
            UUID environmentId,
            EnvironmentDtos.UpdateEnvironmentRequest request
    ) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        if (env.getStatus() == EnvironmentStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_ARCHIVED);
        }
        if (request.domain() != null && !request.domain().isBlank()) {
            validateDomain(request.domain());
        }
        env.update(
                request.domain(),
                request.deploymentStrategy(),
                request.replicaCount(),
                request.cpu(),
                request.memory(),
                request.hpaEnabled(),
                request.gitOpsBranch()
        );
        ServiceEnvironment saved = environmentRepository.save(env);
        auditService.recordSuccess(
                principal,
                AuditAction.UPDATE_ENVIRONMENT,
                "ENVIRONMENT",
                saved.getId(),
                saved.getType().name(),
                saved.getWorkspaceId(),
                "Environment updated"
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        // Sprint A: Deployment 엔티티 미연결 — 항상 soft delete 허용
        env.softDelete();
        environmentRepository.save(env);
        auditService.recordSuccess(
                principal,
                AuditAction.DELETE_ENVIRONMENT,
                "ENVIRONMENT",
                env.getId(),
                env.getType().name(),
                env.getWorkspaceId(),
                "Environment soft-deleted"
        );
    }

    @Transactional
    public EnvironmentDtos.EnvironmentResponse archive(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        if (env.getStatus() == EnvironmentStatus.ARCHIVED) {
            return toResponse(env);
        }
        env.archive();
        ServiceEnvironment saved = environmentRepository.save(env);
        auditService.recordSuccess(
                principal,
                AuditAction.ARCHIVE_ENVIRONMENT,
                "ENVIRONMENT",
                saved.getId(),
                saved.getType().name(),
                saved.getWorkspaceId(),
                "Environment archived"
        );
        return toResponse(saved);
    }

    @Transactional
    public EnvironmentDtos.EnvironmentResponse restore(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        if (env.getStatus() != EnvironmentStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_INVALID_STATE, "Only ARCHIVED environments can be restored");
        }
        env.restoreFromArchive();
        ServiceEnvironment saved = environmentRepository.save(env);
        auditService.recordSuccess(
                principal,
                AuditAction.RESTORE_ENVIRONMENT,
                "ENVIRONMENT",
                saved.getId(),
                saved.getType().name(),
                saved.getWorkspaceId(),
                "Environment restored"
        );
        return toResponse(saved);
    }

    @Transactional
    public EnvironmentDtos.EnvironmentHealthResponse health(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        HealthStatus hs;
        String message;
        if (env.getStatus() == EnvironmentStatus.ARCHIVED) {
            hs = HealthStatus.UNKNOWN;
            message = "Environment is archived";
        } else if (env.getStatus() == EnvironmentStatus.FAILED) {
            hs = HealthStatus.UNHEALTHY;
            message = env.getHealthMessage() != null ? env.getHealthMessage() : "Environment failed";
        } else if (env.getStatus() == EnvironmentStatus.DEPLOYING || env.getStatus() == EnvironmentStatus.CREATING) {
            hs = HealthStatus.DEGRADED;
            message = "Environment is " + env.getStatus().name().toLowerCase(Locale.ROOT);
        } else {
            // READY — 서비스 k8s 상태와 느슨히 연동
            AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(env.getServiceId()).orElse(null);
            if (service != null && service.getK8sStatus() != null) {
                hs = mapK8sHealth(service.getK8sStatus());
                message = "K8s status: " + service.getK8sStatus();
            } else {
                hs = HealthStatus.HEALTHY;
                message = "Environment ready (no live cluster probe in free-only mode)";
            }
        }
        env.applyHealth(hs, message);
        environmentRepository.save(env);
        return new EnvironmentDtos.EnvironmentHealthResponse(
                env.getId(),
                env.getType(),
                env.getStatus(),
                hs,
                message,
                Instant.now(),
                env.getNamespaceName(),
                env.getClusterLabel()
        );
    }

    private AppService requireServiceMember(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
        return service;
    }

    private ServiceEnvironment requireEnvMember(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = environmentRepository.findByIdAndDeletedAtIsNull(environmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
        workspaceBootstrapService.requireMember(env.getWorkspaceId(), principal.userId());
        return env;
    }

    private EnvironmentDtos.EnvironmentResponse toResponse(ServiceEnvironment e) {
        return new EnvironmentDtos.EnvironmentResponse(
                e.getId(),
                e.getServiceId(),
                e.getProjectId(),
                e.getWorkspaceId(),
                e.getType(),
                e.getStatus(),
                e.getNamespaceName(),
                e.getDomain(),
                e.getClusterLabel(),
                e.getDeploymentStrategy(),
                e.getReplicaCount(),
                e.getCpu(),
                e.getMemory(),
                e.getHpaEnabled(),
                e.getGitOpsBranch(),
                e.getHealthStatus(),
                e.getHealthMessage(),
                e.getLastHealthAt(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getArchivedAt()
        );
    }

    public static String defaultNamespace(String serviceName, EnvironmentType type) {
        String base = sanitizeNamespace(serviceName);
        String suffix = type.name().toLowerCase(Locale.ROOT);
        String ns = base + "-" + suffix;
        if (ns.length() > 63) {
            ns = ns.substring(0, 63);
        }
        return ns;
    }

    static String sanitizeNamespace(String raw) {
        String s = raw == null ? "app" : raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (s.isBlank()) {
            s = "app";
        }
        if (s.length() > 50) {
            s = s.substring(0, 50);
        }
        return s;
    }

    private static void validateNamespace(String ns) {
        if (ns == null || !ns.matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid namespace (DNS-1123)");
        }
        if (ns.length() > 63) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Namespace max length is 63");
        }
    }

    private static void validateDomain(String domain) {
        if (!domain.matches("(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$")
                && !domain.matches("(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid domain");
        }
    }

    private static HealthStatus mapK8sHealth(String k8sStatus) {
        if (k8sStatus == null) {
            return HealthStatus.UNKNOWN;
        }
        return switch (k8sStatus.toUpperCase(Locale.ROOT)) {
            case "RUNNING", "READY", "SUCCESS", "SIMULATED" -> HealthStatus.HEALTHY;
            case "FAILED", "ERROR" -> HealthStatus.UNHEALTHY;
            case "PENDING", "DEPLOYING", "PROGRESSING" -> HealthStatus.DEGRADED;
            default -> HealthStatus.UNKNOWN;
        };
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }
}
