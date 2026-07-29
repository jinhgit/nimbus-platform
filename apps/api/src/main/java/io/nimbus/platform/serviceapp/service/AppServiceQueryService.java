package io.nimbus.platform.serviceapp.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.domain.Project;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.dto.ServiceDtos;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppServiceQueryService {

    private final AppServiceRepository appServiceRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public AppServiceQueryService(
            AppServiceRepository appServiceRepository,
            ProjectRepository projectRepository,
            WorkspaceBootstrapService workspaceBootstrapService
    ) {
        this.appServiceRepository = appServiceRepository;
        this.projectRepository = projectRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Transactional(readOnly = true)
    public List<ServiceDtos.ServiceResponse> listByProject(NimbusPrincipal principal, UUID projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        return appServiceRepository.findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceDtos.ServiceResponse> listByWorkspace(NimbusPrincipal principal, UUID workspaceId) {
        UUID effective = workspaceId != null ? workspaceId : principal.workspaceId();
        if (effective == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(effective, principal.userId());
        return appServiceRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(effective)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceDtos.ServiceResponse get(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
        return toResponse(service);
    }

    @Transactional(readOnly = true)
    public long countByWorkspace(UUID workspaceId) {
        return appServiceRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    }

    private ServiceDtos.ServiceResponse toResponse(AppService s) {
        return new ServiceDtos.ServiceResponse(
                s.getId(), s.getName(), s.getDescription(), s.getRuntime(), s.getStatus(),
                s.getEnvironmentType(), s.getReplicaCount(), s.getDatabaseType(), s.getCacheType(),
                s.getHpaEnabled(), s.getProjectId(), s.getWorkspaceId(), s.getTemplateId(),
                s.getWizardId(), s.getGithubRepoUrl(), s.getGithubOwner(), s.getGithubRepoName(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
