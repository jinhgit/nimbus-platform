package io.nimbus.platform.workspace.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.workspace.dto.WorkspaceDtos;
import io.nimbus.platform.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/workspaces")
    public ApiResponse<WorkspaceDtos.WorkspaceResponse> create(
            @Valid @RequestBody WorkspaceDtos.CreateWorkspaceRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.create(principal, request));
    }

    @GetMapping("/workspaces")
    public ApiResponse<List<WorkspaceDtos.WorkspaceSummary>> list() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.listMine(principal));
    }

    @GetMapping("/workspaces/{workspaceId}")
    public ApiResponse<WorkspaceDtos.WorkspaceResponse> get(@PathVariable UUID workspaceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.get(principal, workspaceId));
    }

    @PatchMapping("/workspaces/{workspaceId}")
    public ApiResponse<WorkspaceDtos.WorkspaceResponse> update(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceDtos.UpdateWorkspaceRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.update(principal, workspaceId, request));
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    public ApiResponse<Void> delete(@PathVariable UUID workspaceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        workspaceService.delete(principal, workspaceId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/workspaces/{workspaceId}/members")
    public ApiResponse<List<WorkspaceDtos.MemberResponse>> members(@PathVariable UUID workspaceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.listMembers(principal, workspaceId));
    }

    @PostMapping("/workspaces/{workspaceId}/members/invite")
    public ApiResponse<WorkspaceDtos.MemberResponse> invite(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceDtos.InviteMemberRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.invite(principal, workspaceId, request));
    }

    @PatchMapping("/workspaces/{workspaceId}/members/{memberId}")
    public ApiResponse<WorkspaceDtos.MemberResponse> updateMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody WorkspaceDtos.UpdateMemberRoleRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.updateMemberRole(principal, workspaceId, memberId, request));
    }

    @DeleteMapping("/workspaces/{workspaceId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        workspaceService.removeMember(principal, workspaceId, memberId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/workspaces/{workspaceId}/teams")
    public ApiResponse<List<WorkspaceDtos.TeamResponse>> teams(@PathVariable UUID workspaceId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.listTeams(principal, workspaceId));
    }

    @PostMapping("/workspaces/{workspaceId}/teams")
    public ApiResponse<WorkspaceDtos.TeamResponse> createTeam(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceDtos.CreateTeamRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.createTeam(principal, workspaceId, request));
    }

    @PatchMapping("/teams/{teamId}")
    public ApiResponse<WorkspaceDtos.TeamResponse> updateTeam(
            @PathVariable UUID teamId,
            @Valid @RequestBody WorkspaceDtos.UpdateTeamRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(workspaceService.updateTeam(principal, teamId, request));
    }

    @DeleteMapping("/teams/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable UUID teamId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        workspaceService.deleteTeam(principal, teamId);
        return ApiResponse.ok(null);
    }
}
