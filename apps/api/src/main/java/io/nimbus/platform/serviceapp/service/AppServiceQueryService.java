package io.nimbus.platform.serviceapp.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.domain.Project;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.dto.ServiceDtos;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppServiceQueryService {

    private final AppServiceRepository appServiceRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditService auditService;

    public AppServiceQueryService(
            AppServiceRepository appServiceRepository,
            ProjectRepository projectRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            WorkspacePermissionService workspacePermissionService,
            AuditService auditService
    ) {
        this.appServiceRepository = appServiceRepository;
        this.projectRepository = projectRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.workspacePermissionService = workspacePermissionService;
        this.auditService = auditService;
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
    public List<ServiceDtos.ServiceResponse> listByWorkspace(
            NimbusPrincipal principal,
            UUID workspaceId,
            String tag
    ) {
        UUID effective = workspaceId != null ? workspaceId : principal.workspaceId();
        if (effective == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "workspaceId is required");
        }
        workspaceBootstrapService.requireMember(effective, principal.userId());
        List<AppService> list = appServiceRepository
                .findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(effective);
        if (tag != null && !tag.isBlank()) {
            String needle = tag.trim().toLowerCase(Locale.ROOT);
            list = list.stream()
                    .filter(s -> parseTags(s.getTags()).stream()
                            .anyMatch(t -> t.equalsIgnoreCase(needle)))
                    .toList();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceDtos.ServiceResponse get(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());
        return toResponse(service);
    }

    @Transactional
    public ServiceDtos.ServiceResponse updateTags(
            NimbusPrincipal principal,
            UUID serviceId,
            ServiceDtos.UpdateTagsRequest request
    ) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspacePermissionService.requireMutator(service.getWorkspaceId(), principal.userId());
        String normalized = normalizeTags(request != null ? request.tags() : null);
        service.setTags(normalized);
        AppService saved = appServiceRepository.save(service);
        auditService.recordSuccess(
                principal,
                AuditAction.UPDATE_SERVICE_TAGS,
                "SERVICE",
                saved.getId(),
                saved.getName(),
                saved.getWorkspaceId(),
                "tags=" + (normalized != null ? normalized : "")
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public long countByWorkspace(UUID workspaceId) {
        return appServiceRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    }

    static String normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        String joined = tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", ""))
                .filter(t -> !t.isBlank())
                .distinct()
                .limit(20)
                .collect(Collectors.joining(","));
        return joined.isBlank() ? null : joined;
    }

    static List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private ServiceDtos.ServiceResponse toResponse(AppService s) {
        return new ServiceDtos.ServiceResponse(
                s.getId(), s.getName(), s.getDescription(), s.getRuntime(), s.getStatus(),
                s.getEnvironmentType(), s.getReplicaCount(), s.getDatabaseType(), s.getCacheType(),
                s.getHpaEnabled(), s.getProjectId(), s.getWorkspaceId(), s.getTemplateId(),
                s.getWizardId(), s.getGithubRepoUrl(), s.getGithubOwner(), s.getGithubRepoName(),
                s.getK8sNamespace(), s.getK8sDeployment(), s.getK8sStatus(), s.getK8sClusterType(),
                parseTags(s.getTags()),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
