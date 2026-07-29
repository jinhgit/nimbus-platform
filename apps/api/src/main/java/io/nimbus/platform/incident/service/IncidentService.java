package io.nimbus.platform.incident.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.domain.EnvironmentStatus;
import io.nimbus.platform.environment.domain.HealthStatus;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.incident.domain.Incident;
import io.nimbus.platform.incident.domain.IncidentSource;
import io.nimbus.platform.incident.domain.IncidentStatus;
import io.nimbus.platform.incident.dto.IncidentDtos;
import io.nimbus.platform.incident.repository.IncidentRepository;
import io.nimbus.platform.pipeline.domain.BuildPipeline;
import io.nimbus.platform.pipeline.domain.PipelineStatus;
import io.nimbus.platform.pipeline.repository.BuildPipelineRepository;
import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.domain.SagaStatus;
import io.nimbus.platform.provision.repository.ProvisionSagaRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ProvisionSagaRepository sagaRepository;
    private final BuildPipelineRepository pipelineRepository;
    private final ServiceEnvironmentRepository environmentRepository;
    private final AppServiceRepository appServiceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final WorkspacePermissionService workspacePermissionService;
    private final IncidentAnalysisService analysisService;
    private final AuditService auditService;

    public IncidentService(
            IncidentRepository incidentRepository,
            ProvisionSagaRepository sagaRepository,
            BuildPipelineRepository pipelineRepository,
            ServiceEnvironmentRepository environmentRepository,
            AppServiceRepository appServiceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            WorkspacePermissionService workspacePermissionService,
            IncidentAnalysisService analysisService,
            AuditService auditService
    ) {
        this.incidentRepository = incidentRepository;
        this.sagaRepository = sagaRepository;
        this.pipelineRepository = pipelineRepository;
        this.environmentRepository = environmentRepository;
        this.appServiceRepository = appServiceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.workspacePermissionService = workspacePermissionService;
        this.analysisService = analysisService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IncidentDtos.IncidentResponse> list(
            NimbusPrincipal principal,
            UUID workspaceId,
            IncidentStatus status
    ) {
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(ws, principal.userId());
        List<Incident> items = status != null
                ? incidentRepository.findByWorkspaceIdAndStatusAndDeletedAtIsNullOrderByOpenedAtDesc(ws, status)
                : incidentRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByOpenedAtDesc(ws);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IncidentDtos.IncidentResponse get(NimbusPrincipal principal, UUID id) {
        Incident incident = require(id);
        workspaceBootstrapService.requireMember(incident.getWorkspaceId(), principal.userId());
        return toResponse(incident);
    }

    @Transactional(readOnly = true)
    public IncidentDtos.CountsResponse counts(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(ws, principal.userId());
        return new IncidentDtos.CountsResponse(
                incidentRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, IncidentStatus.OPEN),
                incidentRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, IncidentStatus.ACKNOWLEDGED),
                incidentRepository.countByWorkspaceIdAndStatusAndDeletedAtIsNull(ws, IncidentStatus.RESOLVED)
        );
    }

    /**
     * Scan workspace for failed sagas / pipelines / unhealthy envs and open incidents (deduped).
     */
    @Transactional
    public IncidentDtos.ScanResponse scan(NimbusPrincipal principal, UUID workspaceId) {
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspacePermissionService.requireMutator(ws, principal.userId());

        List<IncidentDtos.IncidentResponse> created = new ArrayList<>();
        int scanned = 0;

        List<ProvisionSaga> failedSagas = sagaRepository.findFailedByWorkspace(
                ws, org.springframework.data.domain.PageRequest.of(0, 50)
        );
        for (ProvisionSaga saga : failedSagas) {
            scanned++;
            if (hasOpen(IncidentSource.SAGA, saga.getId())) continue;
            String serviceName = "wizard:" + (saga.getWizardId() != null
                    ? saga.getWizardId().toString().substring(0, 8)
                    : "unknown");
            String title = "Provision Saga " + saga.getStatus() + " (attempt " + saga.getAttempt() + ")";
            var analysis = analysisService.analyze(
                    IncidentSource.SAGA, title, saga.getFailureReason(), serviceName
            );
            Incident saved = incidentRepository.save(Incident.open(
                    ws, null, serviceName, IncidentSource.SAGA, saga.getId(),
                    title, analysis.severity(), analysis.summary(), analysis.analysisText()
            ));
            created.add(toResponse(saved));
            auditOpen(principal, saved);
        }

        List<BuildPipeline> pipelines =
                pipelineRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(ws);
        for (BuildPipeline p : pipelines) {
            if (p.getStatus() != PipelineStatus.FAILED) continue;
            scanned++;
            if (hasOpen(IncidentSource.PIPELINE, p.getId())) continue;
            String title = "Pipeline FAILED: " + p.getServiceName();
            var analysis = analysisService.analyze(
                    IncidentSource.PIPELINE, title, p.getCurrentStep(), p.getServiceName()
            );
            Incident saved = incidentRepository.save(Incident.open(
                    ws, p.getServiceId(), p.getServiceName(), IncidentSource.PIPELINE, p.getId(),
                    title, analysis.severity(), analysis.summary(), analysis.analysisText()
            ));
            created.add(toResponse(saved));
            auditOpen(principal, saved);
        }

        List<AppService> services =
                appServiceRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(ws);
        for (AppService service : services) {
            List<ServiceEnvironment> envs =
                    environmentRepository.findByServiceIdAndDeletedAtIsNullOrderByCreatedAtAsc(service.getId());
            for (ServiceEnvironment env : envs) {
                boolean bad = env.getStatus() == EnvironmentStatus.FAILED
                        || env.getHealthStatus() == HealthStatus.UNHEALTHY;
                if (!bad) continue;
                scanned++;
                if (hasOpen(IncidentSource.ENVIRONMENT, env.getId())) continue;
                String title = "Environment " + env.getType() + " unhealthy/failed";
                String reason = env.getHealthMessage() != null
                        ? env.getHealthMessage()
                        : env.getStatus().name();
                var analysis = analysisService.analyze(
                        IncidentSource.ENVIRONMENT, title, reason, service.getName()
                );
                Incident saved = incidentRepository.save(Incident.open(
                        ws, service.getId(), service.getName(), IncidentSource.ENVIRONMENT, env.getId(),
                        title, analysis.severity(), analysis.summary(), analysis.analysisText()
                ));
                created.add(toResponse(saved));
                auditOpen(principal, saved);
            }
        }

        return new IncidentDtos.ScanResponse(created.size(), scanned, created);
    }

    @Transactional
    public IncidentDtos.IncidentResponse acknowledge(NimbusPrincipal principal, UUID id) {
        Incident incident = require(id);
        workspacePermissionService.requireMutator(incident.getWorkspaceId(), principal.userId());
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new BusinessException(ErrorCode.INCIDENT_INVALID_STATE, "Already resolved");
        }
        incident.acknowledge(principal.userId());
        incidentRepository.save(incident);
        auditService.recordSuccess(
                principal, AuditAction.ACK_INCIDENT, "INCIDENT",
                incident.getId(), incident.getTitle(), incident.getWorkspaceId(), "Incident acknowledged"
        );
        return toResponse(incident);
    }

    @Transactional
    public IncidentDtos.IncidentResponse resolve(NimbusPrincipal principal, UUID id) {
        Incident incident = require(id);
        workspacePermissionService.requireMutator(incident.getWorkspaceId(), principal.userId());
        incident.resolve();
        incidentRepository.save(incident);
        auditService.recordSuccess(
                principal, AuditAction.RESOLVE_INCIDENT, "INCIDENT",
                incident.getId(), incident.getTitle(), incident.getWorkspaceId(), "Incident resolved"
        );
        return toResponse(incident);
    }

    private boolean hasOpen(IncidentSource source, UUID sourceId) {
        return incidentRepository.existsBySourceTypeAndSourceIdAndStatusNotAndDeletedAtIsNull(
                source, sourceId, IncidentStatus.RESOLVED
        );
    }

    private String resolveServiceName(UUID serviceId, String fallback) {
        if (serviceId == null) return fallback;
        return appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .map(AppService::getName)
                .orElse(fallback);
    }

    private void auditOpen(NimbusPrincipal principal, Incident incident) {
        auditService.recordSuccess(
                principal, AuditAction.OPEN_INCIDENT, "INCIDENT",
                incident.getId(), incident.getTitle(), incident.getWorkspaceId(),
                "Incident opened from " + incident.getSourceType()
        );
    }

    private Incident require(UUID id) {
        return incidentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INCIDENT_NOT_FOUND));
    }

    private IncidentDtos.IncidentResponse toResponse(Incident i) {
        return new IncidentDtos.IncidentResponse(
                i.getId(), i.getWorkspaceId(), i.getServiceId(), i.getServiceName(),
                i.getSourceType(), i.getSourceId(), i.getTitle(), i.getSeverity(), i.getStatus(),
                i.getSummary(), i.getAnalysisText(), i.getProvider(),
                i.getOpenedAt(), i.getResolvedAt()
        );
    }
}
