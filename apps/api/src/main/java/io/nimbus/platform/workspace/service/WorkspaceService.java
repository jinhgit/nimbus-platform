package io.nimbus.platform.workspace.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.domain.User;
import io.nimbus.platform.auth.repository.UserRepository;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.workspace.domain.Team;
import io.nimbus.platform.workspace.domain.Workspace;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.dto.WorkspaceDtos;
import io.nimbus.platform.workspace.repository.TeamRepository;
import io.nimbus.platform.workspace.repository.WorkspaceMemberRepository;
import io.nimbus.platform.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkspaceService {

    private static final Set<WorkspaceRole> CAN_INVITE = EnumSet.of(
            WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.PLATFORM_ENGINEER
    );
    private static final Set<WorkspaceRole> CAN_MANAGE_ROLE = EnumSet.of(
            WorkspaceRole.OWNER, WorkspaceRole.ADMIN
    );

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceBootstrapService bootstrapService;
    private final AuditService auditService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            TeamRepository teamRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            WorkspaceBootstrapService bootstrapService,
            AuditService auditService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.bootstrapService = bootstrapService;
        this.auditService = auditService;
    }

    @Transactional
    public WorkspaceDtos.WorkspaceResponse create(NimbusPrincipal principal, WorkspaceDtos.CreateWorkspaceRequest request) {
        if (workspaceRepository.existsBySlugAndDeletedAtIsNull(request.slug())) {
            throw new BusinessException(ErrorCode.WORKSPACE_SLUG_DUPLICATE);
        }
        Workspace workspace = workspaceRepository.save(
                Workspace.create(request.name(), request.slug(), request.description(), principal.userId())
        );
        Team defaultTeam = teamRepository.save(Team.create(workspace.getId(), "Default", "Default team"));
        memberRepository.save(WorkspaceMember.create(
                workspace.getId(), principal.userId(), WorkspaceRole.OWNER, defaultTeam.getId()
        ));
        auditService.recordSuccess(
                principal,
                AuditAction.CREATE_WORKSPACE,
                "WORKSPACE",
                workspace.getId(),
                workspace.getName(),
                workspace.getId(),
                "워크스페이스 생성"
        );
        return toResponse(workspace, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDtos.WorkspaceSummary> listMine(NimbusPrincipal principal) {
        return memberRepository.findByUserIdAndDeletedAtIsNull(principal.userId()).stream()
                .map(member -> workspaceRepository.findByIdAndDeletedAtIsNull(member.getWorkspaceId())
                        .map(ws -> new WorkspaceDtos.WorkspaceSummary(
                                ws.getId(), ws.getName(), ws.getSlug(), member.getRole()
                        ))
                        .orElse(null))
                .filter(item -> item != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDtos.WorkspaceResponse get(NimbusPrincipal principal, UUID workspaceId) {
        WorkspaceMember member = bootstrapService.requireMember(workspaceId, principal.userId());
        Workspace workspace = requireWorkspace(workspaceId);
        return toResponse(workspace, member.getRole());
    }

    @Transactional
    public WorkspaceDtos.WorkspaceResponse update(
            NimbusPrincipal principal,
            UUID workspaceId,
            WorkspaceDtos.UpdateWorkspaceRequest request
    ) {
        WorkspaceMember member = bootstrapService.requireMember(workspaceId, principal.userId());
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        Workspace workspace = requireWorkspace(workspaceId);
        workspace.update(request.name(), request.description());
        return toResponse(workspaceRepository.save(workspace), member.getRole());
    }

    @Transactional
    public void delete(NimbusPrincipal principal, UUID workspaceId) {
        WorkspaceMember member = bootstrapService.requireMember(workspaceId, principal.userId());
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        if (projectRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId) > 0) {
            throw new BusinessException(ErrorCode.WORKSPACE_HAS_PROJECTS);
        }
        Workspace workspace = requireWorkspace(workspaceId);
        workspace.softDelete();
        workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDtos.MemberResponse> listMembers(NimbusPrincipal principal, UUID workspaceId) {
        bootstrapService.requireMember(workspaceId, principal.userId());
        return memberRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId).stream()
                .map(member -> {
                    User user = userRepository.findByIdAndDeletedAtIsNull(member.getUserId()).orElse(null);
                    return new WorkspaceDtos.MemberResponse(
                            member.getId(),
                            member.getUserId(),
                            user != null ? user.getName() : "Unknown",
                            user != null ? user.getEmail() : "",
                            member.getRole()
                    );
                })
                .toList();
    }

    @Transactional
    public WorkspaceDtos.MemberResponse invite(
            NimbusPrincipal principal,
            UUID workspaceId,
            WorkspaceDtos.InviteMemberRequest request
    ) {
        WorkspaceMember actor = bootstrapService.requireMember(workspaceId, principal.userId());
        if (!CAN_INVITE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        User invitee = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found: " + request.email()));
        if (memberRepository.existsByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, invitee.getId())) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY);
        }
        WorkspaceRole role = request.role() != null ? request.role() : WorkspaceRole.DEVELOPER;
        if (role == WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION, "Cannot invite as OWNER");
        }
        Team defaultTeam = teamRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId).stream()
                .findFirst()
                .orElse(null);
        WorkspaceMember created = memberRepository.save(WorkspaceMember.create(
                workspaceId,
                invitee.getId(),
                role,
                defaultTeam != null ? defaultTeam.getId() : null
        ));
        return new WorkspaceDtos.MemberResponse(
                created.getId(), invitee.getId(), invitee.getName(), invitee.getEmail(), created.getRole()
        );
    }

    @Transactional
    public WorkspaceDtos.MemberResponse updateMemberRole(
            NimbusPrincipal principal,
            UUID workspaceId,
            UUID memberId,
            WorkspaceDtos.UpdateMemberRoleRequest request
    ) {
        WorkspaceMember actor = bootstrapService.requireMember(workspaceId, principal.userId());
        if (!CAN_MANAGE_ROLE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        WorkspaceMember target = memberRepository.findById(memberId)
                .filter(m -> m.getDeletedAt() == null && m.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION, "Cannot change OWNER role");
        }
        if (request.role() == WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION, "Use ownership transfer instead");
        }
        target.changeRole(request.role());
        memberRepository.save(target);
        User user = userRepository.findByIdAndDeletedAtIsNull(target.getUserId()).orElse(null);
        return new WorkspaceDtos.MemberResponse(
                target.getId(),
                target.getUserId(),
                user != null ? user.getName() : "Unknown",
                user != null ? user.getEmail() : "",
                target.getRole()
        );
    }

    @Transactional
    public void removeMember(NimbusPrincipal principal, UUID workspaceId, UUID memberId) {
        WorkspaceMember actor = bootstrapService.requireMember(workspaceId, principal.userId());
        if (!CAN_MANAGE_ROLE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        WorkspaceMember target = memberRepository.findById(memberId)
                .filter(m -> m.getDeletedAt() == null && m.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new BusinessException(ErrorCode.WORKSPACE_LAST_OWNER);
        }
        target.softDelete();
        memberRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDtos.TeamResponse> listTeams(NimbusPrincipal principal, UUID workspaceId) {
        bootstrapService.requireMember(workspaceId, principal.userId());
        return teamRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId).stream()
                .map(this::toTeam)
                .toList();
    }

    @Transactional
    public WorkspaceDtos.TeamResponse createTeam(
            NimbusPrincipal principal,
            UUID workspaceId,
            WorkspaceDtos.CreateTeamRequest request
    ) {
        WorkspaceMember actor = bootstrapService.requireMember(workspaceId, principal.userId());
        if (!CAN_INVITE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        Team team = teamRepository.save(Team.create(workspaceId, request.name(), request.description()));
        return toTeam(team);
    }

    @Transactional
    public WorkspaceDtos.TeamResponse updateTeam(
            NimbusPrincipal principal,
            UUID teamId,
            WorkspaceDtos.UpdateTeamRequest request
    ) {
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        WorkspaceMember actor = bootstrapService.requireMember(team.getWorkspaceId(), principal.userId());
        if (!CAN_INVITE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        team.update(request.name(), request.description());
        return toTeam(teamRepository.save(team));
    }

    @Transactional
    public void deleteTeam(NimbusPrincipal principal, UUID teamId) {
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        WorkspaceMember actor = bootstrapService.requireMember(team.getWorkspaceId(), principal.userId());
        if (!CAN_MANAGE_ROLE.contains(actor.getRole())) {
            throw new BusinessException(ErrorCode.WORKSPACE_PERMISSION);
        }
        if (memberRepository.countByTeamIdAndDeletedAtIsNull(teamId) > 0) {
            throw new BusinessException(ErrorCode.TEAM_HAS_MEMBERS);
        }
        team.softDelete();
        teamRepository.save(team);
    }

    private Workspace requireWorkspace(UUID workspaceId) {
        return workspaceRepository.findByIdAndDeletedAtIsNull(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    private WorkspaceDtos.WorkspaceResponse toResponse(Workspace workspace, WorkspaceRole myRole) {
        return new WorkspaceDtos.WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getDescription(),
                workspace.getOwnerId(),
                memberRepository.countByWorkspaceIdAndDeletedAtIsNull(workspace.getId()),
                projectRepository.countByWorkspaceIdAndDeletedAtIsNull(workspace.getId()),
                myRole
        );
    }

    private WorkspaceDtos.TeamResponse toTeam(Team team) {
        return new WorkspaceDtos.TeamResponse(
                team.getId(), team.getWorkspaceId(), team.getName(), team.getDescription()
        );
    }
}
