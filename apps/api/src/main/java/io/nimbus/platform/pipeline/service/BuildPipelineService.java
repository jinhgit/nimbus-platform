package io.nimbus.platform.pipeline.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.pipeline.domain.BuildPipeline;
import io.nimbus.platform.pipeline.domain.PipelineStatus;
import io.nimbus.platform.pipeline.dto.PipelineDtos;
import io.nimbus.platform.pipeline.repository.BuildPipelineRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BuildPipelineService {

    private final BuildPipelineRepository pipelineRepository;
    private final AppServiceRepository appServiceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final BuildPipelineRunner pipelineRunner;

    public BuildPipelineService(
            BuildPipelineRepository pipelineRepository,
            AppServiceRepository appServiceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            BuildPipelineRunner pipelineRunner
    ) {
        this.pipelineRepository = pipelineRepository;
        this.appServiceRepository = appServiceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.pipelineRunner = pipelineRunner;
    }

    @Transactional
    public PipelineDtos.PipelineResponse createAndRun(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());

        BuildPipeline pipeline = BuildPipeline.create(
                service.getId(),
                service.getProjectId(),
                service.getWorkspaceId(),
                service.getWizardId(),
                service.getName(),
                principal.userId()
        );
        pipeline = pipelineRepository.save(pipeline);
        pipelineRunner.runAsync(pipeline.getId());
        return toResponse(pipeline);
    }

    @Transactional
    public PipelineDtos.PipelineResponse createForWizard(
            UUID serviceId,
            UUID projectId,
            UUID workspaceId,
            UUID wizardId,
            String serviceName,
            UUID userId
    ) {
        BuildPipeline pipeline = BuildPipeline.create(
                serviceId, projectId, workspaceId, wizardId, serviceName, userId
        );
        pipeline = pipelineRepository.save(pipeline);
        pipelineRunner.runAsync(pipeline.getId());
        return toResponse(pipeline);
    }

    @Transactional(readOnly = true)
    public List<PipelineDtos.PipelineResponse> list(NimbusPrincipal principal, UUID workspaceId, UUID serviceId) {
        if (serviceId != null) {
            AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
            workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
            return pipelineRepository.findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(serviceId)
                    .stream().map(this::toResponse).toList();
        }
        UUID ws = workspaceId != null ? workspaceId : principal.workspaceId();
        if (ws == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(ws, principal.userId());
        return pipelineRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(ws)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PipelineDtos.PipelineResponse get(NimbusPrincipal principal, UUID pipelineId) {
        BuildPipeline pipeline = require(pipelineId);
        workspaceBootstrapService.requireMember(pipeline.getWorkspaceId(), principal.userId());
        return toResponse(pipeline);
    }

    @Transactional(readOnly = true)
    public PipelineDtos.PipelineLogsResponse logs(NimbusPrincipal principal, UUID pipelineId) {
        BuildPipeline pipeline = require(pipelineId);
        workspaceBootstrapService.requireMember(pipeline.getWorkspaceId(), principal.userId());
        return new PipelineDtos.PipelineLogsResponse(
                pipeline.getId(),
                pipeline.getStatus(),
                pipeline.getProgress(),
                pipeline.getCurrentStep(),
                pipeline.getLogs()
        );
    }

    @Transactional
    public PipelineDtos.PipelineResponse rerun(NimbusPrincipal principal, UUID pipelineId) {
        BuildPipeline old = require(pipelineId);
        workspaceBootstrapService.requireMember(old.getWorkspaceId(), principal.userId());
        if (old.getStatus() == PipelineStatus.RUNNING || old.getStatus() == PipelineStatus.QUEUED) {
            throw new BusinessException(ErrorCode.PIPELINE_INVALID_STATE, "Pipeline already running");
        }
        return createAndRun(principal, old.getServiceId());
    }

    private BuildPipeline require(UUID id) {
        return pipelineRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PIPELINE_NOT_FOUND));
    }

    private PipelineDtos.PipelineResponse toResponse(BuildPipeline p) {
        return new PipelineDtos.PipelineResponse(
                p.getId(), p.getServiceId(), p.getProjectId(), p.getWorkspaceId(),
                p.getServiceName(), p.getName(), p.getStatus(), p.getProgress(),
                p.getCurrentStep(), p.getImageTag(), p.getDockerfilePath(),
                p.getStartedAt(), p.getFinishedAt(), p.getCreatedAt()
        );
    }
}
