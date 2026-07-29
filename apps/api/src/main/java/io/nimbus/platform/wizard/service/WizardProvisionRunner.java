package io.nimbus.platform.wizard.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.domain.GitRepositoryRecord;
import io.nimbus.platform.github.repository.GitRepositoryRecordRepository;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.github.service.GitHubProvisionService;
import io.nimbus.platform.k8s.domain.K8sDeploymentRecord;
import io.nimbus.platform.k8s.service.K8sDeployService;
import io.nimbus.platform.pipeline.service.BuildPipelineService;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.domain.WizardStatus;
import io.nimbus.platform.wizard.dto.WizardDtos;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

/**
 * Provision orchestrator.
 * 1) GitHub 연결 시 실제 Repo 생성
 * 2) 로컬 k3d/kind 가용 시 실배포 (불가 시 시뮬레이션)
 */
@Component
public class WizardProvisionRunner {

    private static final Logger log = LoggerFactory.getLogger(WizardProvisionRunner.class);

    private final ServiceWizardRepository wizardRepository;
    private final AppServiceRepository appServiceRepository;
    private final GitHubConnectionService connectionService;
    private final GitHubProvisionService gitHubProvisionService;
    private final GitRepositoryRecordRepository gitRepositoryRecordRepository;
    private final K8sDeployService k8sDeployService;
    private final BuildPipelineService buildPipelineService;
    private final WizardService wizardService;
    private final ObjectMapper objectMapper;
    private final boolean autoPipelineOnWizard;

    public WizardProvisionRunner(
            ServiceWizardRepository wizardRepository,
            AppServiceRepository appServiceRepository,
            GitHubConnectionService connectionService,
            GitHubProvisionService gitHubProvisionService,
            GitRepositoryRecordRepository gitRepositoryRecordRepository,
            K8sDeployService k8sDeployService,
            BuildPipelineService buildPipelineService,
            @Lazy WizardService wizardService,
            ObjectMapper objectMapper,
            @Value("${nimbus.pipeline.auto-on-wizard:true}") boolean autoPipelineOnWizard
    ) {
        this.wizardRepository = wizardRepository;
        this.appServiceRepository = appServiceRepository;
        this.connectionService = connectionService;
        this.gitHubProvisionService = gitHubProvisionService;
        this.gitRepositoryRecordRepository = gitRepositoryRecordRepository;
        this.k8sDeployService = k8sDeployService;
        this.buildPipelineService = buildPipelineService;
        this.wizardService = wizardService;
        this.objectMapper = objectMapper;
        this.autoPipelineOnWizard = autoPipelineOnWizard;
    }

    @Async
    public void runAsync(UUID wizardId) {
        try {
            ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
            if (wizard == null) {
                return;
            }
            boolean githubConnected = connectionService.findActiveEntity(wizard.getCreatedBy()).isPresent();
            GitRepositoryRecord repo = null;

            if (githubConnected) {
                repo = runGitHubSteps(wizardId);
                if (repo == null && isFailed(wizardId)) {
                    return;
                }
            } else {
                runGitHubSimulation(wizardId);
            }

            if (isFailed(wizardId) || isCancelled(wizardId)) {
                return;
            }

            K8sDeploymentRecord deploy = runK8sSteps(wizardId);
            complete(wizardId, repo, deploy);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(wizardId, "Interrupted");
        } catch (Exception e) {
            log.error("Provision failed for wizard {}", wizardId, e);
            fail(wizardId, e.getMessage());
        }
    }

    private GitRepositoryRecord runGitHubSteps(UUID wizardId) throws InterruptedException {
        applyStep(wizardId, "GitHub 연결 확인", 5, WizardStatus.PROVISIONING);
        sleep(200);

        ServiceWizard wizard = load(wizardId);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return null;
        }
        WizardDtos.PreviewResponse preview = resolvePreview(wizard);

        applyStep(wizardId, "Repository 생성 중 (GitHub API)", 18, WizardStatus.PROVISIONING);
        GitRepositoryRecord repo;
        try {
            repo = gitHubProvisionService.provisionFromWizard(wizard, preview);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.GITHUB_REPO_EXISTS) {
                fail(wizardId, "Repository 이름이 이미 존재합니다: " + wizard.getServiceName());
                return null;
            }
            throw ex;
        }

        applyStep(wizardId, "README · Dockerfile 커밋", 32, WizardStatus.PROVISIONING);
        sleep(150);
        applyStep(wizardId, "Helm · Terraform · Actions 커밋", 45, WizardStatus.PROVISIONING);
        sleep(150);
        applyStep(wizardId, "ArgoCD · k8s Manifest 커밋", 55, WizardStatus.PROVISIONING);
        sleep(150);
        return repo;
    }

    private void runGitHubSimulation(UUID wizardId) throws InterruptedException {
        applyStep(wizardId, "GitHub 미연결 — SCM 시뮬레이션", 5, WizardStatus.PROVISIONING);
        sleep(300);
        applyStep(wizardId, "Repository 생성 (sim)", 20, WizardStatus.PROVISIONING);
        sleep(350);
        applyStep(wizardId, "GitHub Actions 생성 (sim)", 35, WizardStatus.PROVISIONING);
        sleep(350);
        applyStep(wizardId, "Helm · Terraform 생성 (sim)", 48, WizardStatus.PROVISIONING);
        sleep(300);
        applyStep(wizardId, "ArgoCD Manifest 생성 (sim)", 55, WizardStatus.PROVISIONING);
        sleep(250);
    }

    private K8sDeploymentRecord runK8sSteps(UUID wizardId) throws InterruptedException {
        applyStep(wizardId, "로컬 Kubernetes 연결 확인 (k3d/kind)", 62, WizardStatus.DEPLOYING);
        sleep(200);

        ServiceWizard wizard = load(wizardId);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return null;
        }

        boolean available = k8sDeployService.isClusterAvailable();
        if (available) {
            applyStep(wizardId, "Namespace · Deployment 적용", 75, WizardStatus.DEPLOYING);
        } else {
            applyStep(wizardId, "클러스터 없음 — K8s 시뮬레이션", 75, WizardStatus.DEPLOYING);
        }

        K8sDeploymentRecord deploy = k8sDeployService.deployFromWizard(wizard);

        applyStep(wizardId,
                available
                        ? "Pod Ready 대기 (" + deploy.getReadyReplicas() + "/" + deploy.getReplicas() + ")"
                        : "Deploy 시뮬레이션 완료",
                90, WizardStatus.DEPLOYING);
        sleep(200);
        applyStep(wizardId, "Health Verify", 100, WizardStatus.DEPLOYING);
        return deploy;
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
    protected void complete(UUID wizardId, GitRepositoryRecord repo, K8sDeploymentRecord deploy) {
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
        }
        if (deploy != null) {
            service.bindK8s(
                    deploy.getNamespaceName(),
                    deploy.getDeploymentName(),
                    deploy.getStatus().name(),
                    deploy.getClusterType()
            );
        }
        service.markReady();
        service = appServiceRepository.save(service);

        if (repo != null) {
            repo.bindService(service.getId());
            gitRepositoryRecordRepository.save(repo);
            wizard.appendLogPublic("GitHub Repository: " + repo.getHtmlUrl());
        } else {
            wizard.appendLogPublic("GitHub not connected — SCM simulation");
        }

        if (deploy != null) {
            k8sDeployService.bindService(wizardId, service.getId());
            wizard.appendLogPublic("K8s: " + deploy.getStatus()
                    + " ns=" + deploy.getNamespaceName()
                    + " deploy=" + deploy.getDeploymentName()
                    + " type=" + deploy.getClusterType());
        }

        wizard.complete(service.getId());
        wizardRepository.save(wizard);

        // 이미지 빌드 파이프라인 자동 트리거 (async)
        if (autoPipelineOnWizard) {
            try {
                buildPipelineService.createForWizard(
                        service.getId(),
                        service.getProjectId(),
                        service.getWorkspaceId(),
                        wizard.getId(),
                        service.getName(),
                        wizard.getCreatedBy()
                );
                wizard.appendLogPublic("Image build pipeline queued");
                wizardRepository.save(wizard);
            } catch (Exception e) {
                log.warn("Failed to queue build pipeline: {}", e.getMessage());
            }
        }

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

    private boolean isFailed(UUID wizardId) {
        ServiceWizard w = load(wizardId);
        return w != null && w.getStatus() == WizardStatus.FAILED;
    }

    private boolean isCancelled(UUID wizardId) {
        ServiceWizard w = load(wizardId);
        return w != null && w.getStatus() == WizardStatus.CANCELLED;
    }

    private ServiceWizard load(UUID wizardId) {
        return wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
