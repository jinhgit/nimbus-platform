package io.nimbus.platform.project.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.domain.Project;
import io.nimbus.platform.project.domain.ProjectStatus;
import io.nimbus.platform.project.domain.Visibility;
import io.nimbus.platform.project.dto.ProjectDtos;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Set<WorkspaceRole> CAN_CREATE = EnumSet.of(
            WorkspaceRole.OWNER,
            WorkspaceRole.ADMIN,
            WorkspaceRole.PLATFORM_ENGINEER,
            WorkspaceRole.DEVELOPER
    );

    private final ProjectRepository projectRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceBootstrapService workspaceBootstrapService
    ) {
        this.projectRepository = projectRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(NimbusPrincipal principal, ProjectDtos.CreateProjectRequest request) {
        var member = workspaceBootstrapService.requireMember(request.workspaceId(), principal.userId());
        if (!CAN_CREATE.contains(member.getRole())) {
            throw new BusinessException(ErrorCode.PROJECT_PERMISSION);
        }
        if (projectRepository.existsByWorkspaceIdAndNameAndDeletedAtIsNull(request.workspaceId(), request.name())) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE);
        }
        Project project = projectRepository.save(Project.create(
                request.workspaceId(),
                request.teamId(),
                request.name(),
                request.description(),
                request.visibility() != null ? request.visibility() : Visibility.PRIVATE,
                principal.userId()
        ));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> list(NimbusPrincipal principal, UUID workspaceId) {
        UUID effectiveWorkspace = workspaceId != null ? workspaceId : principal.workspaceId();
        if (effectiveWorkspace == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(effectiveWorkspace, principal.userId());
        return projectRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(effectiveWorkspace)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse get(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        return toResponse(project);
    }

    @Transactional
    public ProjectDtos.ProjectResponse update(
            NimbusPrincipal principal,
            UUID projectId,
            ProjectDtos.UpdateProjectRequest request
    ) {
        Project project = requireProject(projectId);
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
        }
        if (request.name() != null
                && !request.name().equals(project.getName())
                && projectRepository.existsByWorkspaceIdAndNameAndDeletedAtIsNull(project.getWorkspaceId(), request.name())) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE);
        }
        project.update(request.name(), request.description(), request.teamId(), request.visibility());
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public void delete(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        project.softDelete();
        projectRepository.save(project);
    }

    @Transactional
    public ProjectDtos.ProjectResponse archive(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        project.archive();
        return toResponse(projectRepository.save(project));
    }

    @Transactional
    public ProjectDtos.ProjectResponse restore(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        project.restore();
        return toResponse(projectRepository.save(project));
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private ProjectDtos.ProjectResponse toResponse(Project project) {
        return new ProjectDtos.ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getVisibility(),
                project.getWorkspaceId(),
                project.getTeamId(),
                project.getOwnerId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
