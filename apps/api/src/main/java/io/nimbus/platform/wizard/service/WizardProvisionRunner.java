package io.nimbus.platform.wizard.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.domain.GitRepositoryRecord;
import io.nimbus.platform.github.repository.GitRepositoryRecordRepository;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.github.service.GitHubProvisionService;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.domain.WizardStatus;
import io.nimbus.platform.wizard.dto.WizardDtos;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Provision orchestrator.
 * GitHub 연결 시: 실제 Repository + 파일 생성.
 * 미연결 시: 시연용 시뮬레이션 (free-only 로컬).
 */
@Component
public class WizardProvisionRunner {

    private static final Logger log = LoggerFactory.getLogger(WizardProvisionRunner.class);

    private final ServiceWizardRepository wizardRepository;
    private final AppServiceRepository appServiceRepository;
    private final GitHubConnectionService connectionService;
    private final GitHubProvisionService gitHubProvisionService;
    private final GitRepositoryRecordRepository gitRepositoryRecordRepository;
    private final WizardService wizardService;
    private final ObjectMapper objectMapper;

    public WizardProvisionRunner(
            ServiceWizardRepository wizardRepository,
            AppServiceRepository appServiceRepository,
            GitHubConnectionService connectionService,
            GitHubProvisionService gitHubProvisionService,
            GitRepositoryRecordRepository gitRepositoryRecordRepository,
            @Lazy WizardService wizardService,
            ObjectMapper objectMapper
    ) {
        this.wizardRepository = wizardRepository;
        this.appServiceRepository = appServiceRepository;
        this.connectionService = connectionService;
        this.gitHubProvisionService = gitHubProvisionService;
        this.gitRepositoryRecordRepository = gitRepositoryRecordRepository;
        this.wizardService = wizardService;
        this.objectMapper = objectMapper;
    }

    @Async
    public void runAsync(UUID wizardId) {
        try {
            ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
            if (wizard == null) {
                return;
            }
            boolean githubConnected = connectionService.findActiveEntity(wizard.getCreatedBy()).isPresent();
            if (githubConnected) {
                runRealGitHub(wizardId);
            } else {
                runSimulation(wizardId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(wizardId, "Interrupted");
        } catch (Exception e) {
            log.error("Provision failed for wizard {}", wizardId, e);
            fail(wizardId, e.getMessage());
        }
    }

    private void runRealGitHub(UUID wizardId) throws InterruptedException {
        applyStep(wizardId, "GitHub 연결 확인", 5, WizardStatus.PROVISIONING);
        sleep(300);

        ServiceWizard wizard = load(wizardId);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return;
        }

        WizardDtos.PreviewResponse preview = resolvePreview(wizard);

        applyStep(wizardId, "Repository 생성 중 (GitHub API)", 20, WizardStatus.PROVISIONING);
        GitRepositoryRecord repo;
        try {
            repo = gitHubProvisionService.provisionFromWizard(wizard, preview);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.GITHUB_REPO_EXISTS) {
                fail(wizardId, "Repository 이름이 이미 존재합니다: " + wizard.getServiceName());
                return;
            }
            throw ex;
        }

        applyStep(wizardId, "README · Dockerfile 커밋", 40, WizardStatus.PROVISIONING);
        sleep(200);
        applyStep(wizardId, "Helm · Terraform · Actions 커밋", 60, WizardStatus.PROVISIONING);
        sleep(200);
        applyStep(wizardId, "ArgoCD · k8s Manifest 커밋", 75, WizardStatus.PROVISIONING);
        sleep(200);
        applyStep(wizardId, "Deploy (local simulation)", 90, WizardStatus.DEPLOYING);
        sleep(300);
        applyStep(wizardId, "Verify Health", 100, WizardStatus.DEPLOYING);

        complete(wizardId, repo);
        log.info("Wizard {} provisioned real GitHub repo {}", wizardId, repo.getHtmlUrl());
    }

    private void runSimulation(UUID wizardId) throws InterruptedException {
        applyStep(wizardId, "GitHub 미연결 — 시뮬레이션 모드", 5, WizardStatus.PROVISIONING);
        sleep(400);
        applyStep(wizardId, "Repository 생성 (sim)", 20, WizardStatus.PROVISIONING);
        sleep(500);
        applyStep(wizardId, "GitHub Actions 생성 (sim)", 40, WizardStatus.PROVISIONING);
        sleep(500);
        applyStep(wizardId, "Helm · Terraform 생성 (sim)", 60, WizardStatus.PROVISIONING);
        sleep(500);
        applyStep(wizardId, "ArgoCD Manifest 생성 (sim)", 75, WizardStatus.PROVISIONING);
        sleep(400);
        applyStep(wizardId, "Deploy (local simulation)", 90, WizardStatus.DEPLOYING);
        sleep(400);
        applyStep(wizardId, "Verify Health", 100, WizardStatus.DEPLOYING);
        complete(wizardId, null);
    }

    private WizardDtos.PreviewResponse resolvePreview(ServiceWizard wizard) {
        if (wizard.getPreviewJson() != null && !wizard.getPreviewJson().isBlank()) {
            try {
                return objectMapper.readValue(wizard.getPreviewJson(), WizardDtos.PreviewResponse.class);
            } catch (Exception ignored) {
                // rebuild
            }
        }
        return wizardService.buildPreview(wizard);
    }

    @Transactional
    protected void applyStep(UUID wizardId, String message, int progress, WizardStatus status) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return;
        }
        wizard.updateProgress(progress, message, status);
        wizardRepository.save(wizard);
    }

    @Transactional
    protected void complete(UUID wizardId, GitRepositoryRecord repo) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return;
        }
        AppService service = AppService.create(
                wizard.getProjectId(),
                wizard.getWorkspaceId(),
                wizard.getServiceName(),
                "Created via Service Wizard",
                wizard.getRuntime(),
                wizard.getTemplateId(),
                wizard.getEnvironmentType(),
                wizard.getReplicaCount(),
                wizard.getDatabaseType(),
                wizard.getCacheType(),
                wizard.getHpaEnabled(),
                wizard.getId(),
                wizard.getCreatedBy()
        );
        if (repo != null) {
            service.bindGitHub(repo.getOwner(), repo.getRepoName(), repo.getHtmlUrl());
            service.markReady();
            service = appServiceRepository.save(service);
            repo.bindService(service.getId());
            gitRepositoryRecordRepository.save(repo);
        } else {
            service.markReady();
            service = appServiceRepository.save(service);
        }
        wizard.complete(service.getId());
        if (repo != null) {
            wizard.appendLogPublic("GitHub Repository: " + repo.getHtmlUrl());
        } else {
            wizard.appendLogPublic("GitHub not connected — simulation only");
        }
        wizardRepository.save(wizard);
        log.info("Wizard {} completed → service {}", wizardId, service.getId());
    }

    @Transactional
    protected void fail(UUID wizardId, String reason) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
        if (wizard == null) {
            return;
        }
        wizard.fail(reason != null ? reason : "Unknown error");
        wizardRepository.save(wizard);
    }

    private ServiceWizard load(UUID wizardId) {
        return wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
