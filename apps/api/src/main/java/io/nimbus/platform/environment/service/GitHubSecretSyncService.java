package io.nimbus.platform.environment.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.domain.EnvSecret;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.dto.ConfigDtos;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.github.crypto.GitHubActionsSecretCipher;
import io.nimbus.platform.github.crypto.TokenCryptoService;
import io.nimbus.platform.github.domain.GitHubConnection;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sprint D thin GitHub Actions secret sync.
 * LIVE when SCM + repo + sealed-box available; otherwise SIMULATED (still audited).
 */
@Service
public class GitHubSecretSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSecretSyncService.class);

    private final ServiceEnvironmentRepository environmentRepository;
    private final AppServiceRepository appServiceRepository;
    private final EnvironmentConfigService configService;
    private final GitHubConnectionService connectionService;
    private final GitProvider gitProvider;
    private final TokenCryptoService tokenCryptoService;
    private final WorkspacePermissionService workspacePermissionService;
    private final AuditService auditService;

    public GitHubSecretSyncService(
            ServiceEnvironmentRepository environmentRepository,
            AppServiceRepository appServiceRepository,
            EnvironmentConfigService configService,
            GitHubConnectionService connectionService,
            GitProvider gitProvider,
            TokenCryptoService tokenCryptoService,
            WorkspacePermissionService workspacePermissionService,
            AuditService auditService
    ) {
        this.environmentRepository = environmentRepository;
        this.appServiceRepository = appServiceRepository;
        this.configService = configService;
        this.connectionService = connectionService;
        this.gitProvider = gitProvider;
        this.tokenCryptoService = tokenCryptoService;
        this.workspacePermissionService = workspacePermissionService;
        this.auditService = auditService;
    }

    @Transactional
    public ConfigDtos.SecretSyncResponse sync(
            NimbusPrincipal principal,
            java.util.UUID environmentId,
            ConfigDtos.SecretSyncRequest request
    ) {
        ServiceEnvironment env = environmentRepository.findByIdAndDeletedAtIsNull(environmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
        workspacePermissionService.requireMutator(env.getWorkspaceId(), principal.userId());

        AppService service = appServiceRepository.findByIdAndDeletedAtIsNull(env.getServiceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

        List<EnvSecret> secrets = configService.listSecretEntities(environmentId);
        if (request != null && request.keys() != null && !request.keys().isEmpty()) {
            Set<String> wanted = request.keys().stream()
                    .map(k -> k.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            secrets = secrets.stream().filter(s -> wanted.contains(s.getKey())).toList();
        }

        String owner = service.getGithubOwner();
        String repo = service.getGithubRepoName();
        String repositoryLabel = (owner != null && repo != null) ? owner + "/" + repo : null;

        Optional<GitHubConnection> connection = connectionService.findActiveEntity(principal.userId());
        boolean canLive = connection.isPresent()
                && owner != null && !owner.isBlank()
                && repo != null && !repo.isBlank();

        List<ConfigDtos.SecretSyncItem> items = new ArrayList<>();
        int ok = 0;
        int fail = 0;
        String mode;

        if (!canLive) {
            mode = "SIMULATED";
            String why = connection.isEmpty()
                    ? "GitHub SCM 미연결"
                    : "서비스에 GitHub repo 바인딩 없음";
            for (EnvSecret secret : secrets) {
                configService.markSecretSynced(secret, "SIMULATED", why);
                items.add(new ConfigDtos.SecretSyncItem(secret.getKey(), "SIMULATED", why));
                ok++;
            }
        } else {
            String token = connectionService.decryptToken(connection.get());
            GitProvider.RepoPublicKey publicKey;
            try {
                publicKey = gitProvider.fetchActionsPublicKey(token, owner, repo);
            } catch (Exception ex) {
                log.warn("GitHub public-key fetch failed: {}", ex.getMessage());
                mode = "SIMULATED";
                String why = "공개키 조회 실패: " + ex.getMessage();
                for (EnvSecret secret : secrets) {
                    configService.markSecretSynced(secret, "FAILED", why);
                    items.add(new ConfigDtos.SecretSyncItem(secret.getKey(), "FAILED", why));
                    fail++;
                }
                auditService.recordSuccess(
                        principal, AuditAction.SYNC_GITHUB_SECRETS, "ENVIRONMENT",
                        env.getId(), repositoryLabel, env.getWorkspaceId(),
                        "GitHub secret sync failed: " + why
                );
                return new ConfigDtos.SecretSyncResponse(
                        mode, repositoryLabel, secrets.size(), 0, fail, items, why
                );
            }

            mode = "LIVE";
            boolean anySealed = false;
            for (EnvSecret secret : secrets) {
                try {
                    String plain = tokenCryptoService.decrypt(secret.getValueEnc());
                    Optional<String> sealed = GitHubActionsSecretCipher.trySeal(plain, publicKey.keyBase64());
                    if (sealed.isEmpty()) {
                        // Thin: public key OK but sealed-box not in this build → intent recorded
                        String msg = "공개키 확인됨 (key_id=" + publicKey.keyId()
                                + "). sealed-box 미포함 빌드 — 원격 푸시 생략(SIMULATED_LIVE)";
                        configService.markSecretSynced(secret, "SIMULATED_LIVE", msg);
                        items.add(new ConfigDtos.SecretSyncItem(secret.getKey(), "SIMULATED_LIVE", msg));
                        ok++;
                    } else {
                        gitProvider.putActionsSecret(
                                token, owner, repo, secret.getKey(), sealed.get(), publicKey.keyId()
                        );
                        configService.markSecretSynced(secret, "SYNCED", "Pushed to GitHub Actions");
                        items.add(new ConfigDtos.SecretSyncItem(secret.getKey(), "SYNCED", "Pushed"));
                        ok++;
                        anySealed = true;
                    }
                } catch (Exception ex) {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "sync error";
                    configService.markSecretSynced(secret, "FAILED", msg);
                    items.add(new ConfigDtos.SecretSyncItem(secret.getKey(), "FAILED", msg));
                    fail++;
                }
            }
            if (!anySealed && fail == 0) {
                mode = "SIMULATED_LIVE";
            }
        }

        String summary = mode + " repo=" + (repositoryLabel != null ? repositoryLabel : "—")
                + " ok=" + ok + " fail=" + fail;
        auditService.recordSuccess(
                principal, AuditAction.SYNC_GITHUB_SECRETS, "ENVIRONMENT",
                env.getId(), repositoryLabel != null ? repositoryLabel : env.getType().name(),
                env.getWorkspaceId(),
                summary
        );

        return new ConfigDtos.SecretSyncResponse(
                mode,
                repositoryLabel,
                secrets.size(),
                ok,
                fail,
                items,
                summary
        );
    }
}
