package io.nimbus.platform.project.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.project.dto.ProjectDtos;
import io.nimbus.platform.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ApiResponse<ProjectDtos.ProjectResponse> create(
            @Valid @RequestBody ProjectDtos.CreateProjectRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.create(principal, request));
    }

    @GetMapping
    public ApiResponse<List<ProjectDtos.ProjectResponse>> list(
            @RequestParam(required = false) UUID workspaceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.list(principal, workspaceId));
    }

    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDtos.ProjectResponse> get(@PathVariable UUID projectId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.get(principal, projectId));
    }

    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectDtos.ProjectResponse> update(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectDtos.UpdateProjectRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.update(principal, projectId, request));
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable UUID projectId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        projectService.delete(principal, projectId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{projectId}/archive")
    public ApiResponse<ProjectDtos.ProjectResponse> archive(@PathVariable UUID projectId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.archive(principal, projectId));
    }

    @PostMapping("/{projectId}/restore")
    public ApiResponse<ProjectDtos.ProjectResponse> restore(@PathVariable UUID projectId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(projectService.restore(principal, projectId));
    }
}
