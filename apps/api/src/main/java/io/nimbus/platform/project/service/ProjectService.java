package io.nimbus.platform.project.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.domain.Project;
import io.nimbus.platform.project.domain.ProjectStatus;
import io.nimbus.platform.project.domain.Visibility;
import io.nimbus.platform.project.dto.ProjectDtos;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditService auditService;

    public ProjectService(
            ProjectRepository projectRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            WorkspacePermissionService workspacePermissionService,
            AuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.workspacePermissionService = workspacePermissionService;
        this.auditService = auditService;
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(NimbusPrincipal principal, ProjectDtos.CreateProjectRequest request) {
        workspacePermissionService.requireMutator(request.workspaceId(), principal.userId());
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
        auditService.recordSuccess(
                principal,
                AuditAction.CREATE_PROJECT,
                "PROJECT",
                project.getId(),
                project.getName(),
                project.getWorkspaceId(),
                "프로젝트 생성"
        );
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
        workspacePermissionService.requireMutator(project.getWorkspaceId(), principal.userId());
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
        }
        if (request.name() != null
                && !request.name().equals(project.getName())
                && projectRepository.existsByWorkspaceIdAndNameAndDeletedAtIsNull(project.getWorkspaceId(), request.name())) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_DUPLICATE);
        }
        project.update(request.name(), request.description(), request.teamId(), request.visibility());
        Project saved = projectRepository.save(project);
        auditService.recordSuccess(
                principal,
                AuditAction.UPDATE_PROJECT,
                "PROJECT",
                saved.getId(),
                saved.getName(),
                saved.getWorkspaceId(),
                "프로젝트 수정"
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspacePermissionService.requireMutator(project.getWorkspaceId(), principal.userId());
        project.softDelete();
        projectRepository.save(project);
        auditService.recordSuccess(
                principal,
                AuditAction.DELETE_PROJECT,
                "PROJECT",
                project.getId(),
                project.getName(),
                project.getWorkspaceId(),
                "프로젝트 삭제(soft)"
        );
    }

    @Transactional
    public ProjectDtos.ProjectResponse archive(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspacePermissionService.requireMutator(project.getWorkspaceId(), principal.userId());
        project.archive();
        Project saved = projectRepository.save(project);
        auditService.recordSuccess(
                principal,
                AuditAction.ARCHIVE_PROJECT,
                "PROJECT",
                saved.getId(),
                saved.getName(),
                saved.getWorkspaceId(),
                "프로젝트 아카이브"
        );
        return toResponse(saved);
    }

    @Transactional
    public ProjectDtos.ProjectResponse restore(NimbusPrincipal principal, UUID projectId) {
        Project project = requireProject(projectId);
        workspacePermissionService.requireMutator(project.getWorkspaceId(), principal.userId());
        project.restore();
        Project saved = projectRepository.save(project);
        auditService.recordSuccess(
                principal,
                AuditAction.RESTORE_PROJECT,
                "PROJECT",
                saved.getId(),
                saved.getName(),
                saved.getWorkspaceId(),
                "프로젝트 복원"
        );
        return toResponse(saved);
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
