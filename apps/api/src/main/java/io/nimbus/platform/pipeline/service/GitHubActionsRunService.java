package io.nimbus.platform.pipeline.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.domain.GitHubConnection;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.pipeline.dto.PipelineDtos;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thin GitHub Actions run list — LIVE when SCM+repo bound, else SIMULATED empty.
 */
@Service
public class GitHubActionsRunService {

    private static final Logger log = LoggerFactory.getLogger(GitHubActionsRunService.class);

    private final AppServiceRepository appServiceRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final GitHubConnectionService connectionService;
    private final GitProvider gitProvider;

    public GitHubActionsRunService(
            AppServiceRepository appServiceRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            GitHubConnectionService connectionService,
            GitProvider gitProvider
    ) {
        this.appServiceRepository = appServiceRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.connectionService = connectionService;
        this.gitProvider = gitProvider;
    }

    @Transactional(readOnly = true)
    public PipelineDtos.GithubRunsResponse listForService(NimbusPrincipal principal, UUID serviceId) {
        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
        workspaceBootstrapService.requireMember(service.getWorkspaceId(), principal.userId());

        String owner = service.getGithubOwner();
        String repo = service.getGithubRepoName();
        String repository = (owner != null && repo != null) ? owner + "/" + repo : null;

        Optional<GitHubConnection> connection = connectionService.findActiveEntity(principal.userId());
        boolean canLive = connection.isPresent()
                && owner != null && !owner.isBlank()
                && repo != null && !repo.isBlank();

        if (!canLive) {
            String why = connection.isEmpty()
                    ? "GitHub SCM 미연결 — 로컬 시뮬 파이프라인만 표시"
                    : "서비스에 GitHub repo 바인딩 없음";
            return new PipelineDtos.GithubRunsResponse(
                    serviceId, repository, "SIMULATED", why, List.of()
            );
        }

        try {
            String token = connectionService.decryptToken(connection.get());
            List<GitProvider.WorkflowRun> runs = gitProvider.listWorkflowRuns(token, owner, repo, 10);
            List<PipelineDtos.GithubWorkflowRun> items = runs.stream()
                    .map(r -> new PipelineDtos.GithubWorkflowRun(
                            r.id(), r.name(), r.status(), r.conclusion(),
                            r.htmlUrl(), r.headBranch(), r.event(),
                            r.createdAt(), r.updatedAt()
                    ))
                    .toList();
            return new PipelineDtos.GithubRunsResponse(
                    serviceId,
                    repository,
                    "LIVE",
                    items.isEmpty() ? "워크플로 런이 없습니다" : items.size() + " runs",
                    items
            );
        } catch (Exception ex) {
            log.info("GitHub Actions list failed → SIMULATED: {}", ex.getMessage());
            return new PipelineDtos.GithubRunsResponse(
                    serviceId,
                    repository,
                    "SIMULATED",
                    "Actions API 실패: " + truncate(ex.getMessage()),
                    List.of()
            );
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) : s;
    }
}
