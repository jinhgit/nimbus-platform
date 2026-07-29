package io.nimbus.platform.workspace.service;

import io.nimbus.platform.auth.domain.User;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.workspace.domain.Team;
import io.nimbus.platform.workspace.domain.Workspace;
import io.nimbus.platform.workspace.domain.WorkspaceMember;
import io.nimbus.platform.workspace.domain.WorkspaceRole;
import io.nimbus.platform.workspace.repository.TeamRepository;
import io.nimbus.platform.workspace.repository.WorkspaceMemberRepository;
import io.nimbus.platform.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class WorkspaceBootstrapService {

    private final WorkspaceRepository workspaceRepository;
    private final TeamRepository teamRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceBootstrapService(
            WorkspaceRepository workspaceRepository,
            TeamRepository teamRepository,
            WorkspaceMemberRepository memberRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Workspace createPersonalWorkspace(User user) {
        String baseSlug = slugify(user.getName() + "-ws");
        String slug = uniqueSlug(baseSlug);
        Workspace workspace = workspaceRepository.save(
                Workspace.create(user.getName() + " Workspace", slug, "Personal workspace", user.getId())
        );
        Team defaultTeam = teamRepository.save(Team.create(workspace.getId(), "Default", "Default team"));
        memberRepository.save(WorkspaceMember.create(
                workspace.getId(),
                user.getId(),
                WorkspaceRole.OWNER,
                defaultTeam.getId()
        ));
        return workspace;
    }

    @Transactional(readOnly = true)
    public WorkspaceMember requireMember(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspaceIdAndUserIdAndDeletedAtIsNull(workspaceId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_WORKSPACE_DENIED));
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int i = 1;
        while (workspaceRepository.existsBySlugAndDeletedAtIsNull(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    private static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "workspace";
        }
        if (slug.length() > 40) {
            slug = slug.substring(0, 40);
        }
        return slug;
    }
}
