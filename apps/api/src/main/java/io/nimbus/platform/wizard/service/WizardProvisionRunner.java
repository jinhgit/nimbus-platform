package io.nimbus.platform.wizard.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.service.EnvironmentService;
import io.nimbus.platform.github.domain.GitRepositoryRecord;
import io.nimbus.platform.github.repository.GitRepositoryRecordRepository;
import io.nimbus.platform.github.service.GitHubConnectionService;
import io.nimbus.platform.github.service.GitHubProvisionService;
import io.nimbus.platform.k8s.domain.K8sDeploymentRecord;
import io.nimbus.platform.k8s.service.K8sDeployService;
import io.nimbus.platform.pipeline.service.BuildPipelineService;
import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.domain.StepCode;
import io.nimbus.platform.provision.service.ProvisionSagaService;
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
 * Provision orchestrator with persisted Saga steps (Sprint C).
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
    private final EnvironmentService environmentService;
    private final ProvisionSagaService sagaService;
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
            EnvironmentService environmentService,
            ProvisionSagaService sagaService,
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
        this.environmentService = environmentService;
        this.sagaService = sagaService;
        this.wizardService = wizardService;
        this.objectMapper = objectMapper;
        this.autoPipelineOnWizard = autoPipelineOnWizard;
    }

    /**
     * 동기 진입: Saga 생성 후 비동기 실행.
     */
    @Transactional
    public UUID startSaga(ServiceWizard wizard) {
        ProvisionSaga saga = sagaService.createForWizard(wizard);
        return saga.getId();
    }

    @Async
    public void runAsync(UUID wizardId, UUID sagaId) {
        try {
            sagaService.markRunning(sagaId);
            ServiceWizard wizard = load(wizardId);
            if (wizard == null) {
                return;
            }
            if (wizard.getStatus() == WizardStatus.CANCELLED) {
                return;
            }

            boolean githubConnected = connectionService.findActiveEntity(wizard.getCreatedBy()).isPresent();
            GitRepositoryRecord repo = null;

            // SCM_PREPARE
            stepRun(wizardId, sagaId, StepCode.SCM_PREPARE, "GitHub 연결 확인", 5, WizardStatus.PROVISIONING);
            sleep(150);
            if (abort(wizardId)) {
                return;
            }
            sagaService.succeedStep(sagaId, StepCode.SCM_PREPARE,
                    githubConnected ? "GitHub connected" : "No SCM — simulation path");

            // SCM_REPO
            stepRun(wizardId, sagaId, StepCode.SCM_REPO,
                    githubConnected ? "Repository 생성 중 (GitHub API)" : "Repository 생성 (sim)",
                    20, WizardStatus.PROVISIONING);
            sleep(200);
            if (abort(wizardId)) {
                return;
            }
            if (githubConnected) {
                try {
                    WizardDtos.PreviewResponse preview = resolvePreview(load(wizardId));
                    repo = gitHubProvisionService.provisionFromWizard(load(wizardId), preview);
                    sagaService.succeedStep(sagaId, StepCode.SCM_REPO, "Repo: " + repo.getHtmlUrl());
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() == ErrorCode.GITHUB_REPO_EXISTS) {
                        failSaga(wizardId, sagaId, StepCode.SCM_REPO,
                                "Repository 이름이 이미 존재합니다: " + wizard.getServiceName());
                        return;
                    }
                    failSaga(wizardId, sagaId, StepCode.SCM_REPO, ex.getMessage());
                    return;
                }
            } else {
                sleep(250);
                sagaService.succeedStep(sagaId, StepCode.SCM_REPO, "Simulated repository");
            }

            // SCM_MANIFESTS
            stepRun(wizardId, sagaId, StepCode.SCM_MANIFESTS,
                    githubConnected ? "Helm · Terraform · Actions · Argo 커밋" : "Manifest 생성 (sim)",
                    45, WizardStatus.PROVISIONING);
            sleep(300);
            if (abort(wizardId)) {
                return;
            }
            sagaService.succeedStep(sagaId, StepCode.SCM_MANIFESTS,
                    githubConnected ? "Manifests committed" : "Manifests simulated");

            // K8S_PREPARE
            stepRun(wizardId, sagaId, StepCode.K8S_PREPARE,
                    "로컬 Kubernetes 연결 확인 (k3d/kind)", 62, WizardStatus.DEPLOYING);
            sleep(150);
            if (abort(wizardId)) {
                return;
            }
            boolean available = k8sDeployService.isClusterAvailable();
            sagaService.succeedStep(sagaId, StepCode.K8S_PREPARE,
                    available ? "Cluster available" : "Cluster unavailable — sim deploy");

            // K8S_DEPLOY
            stepRun(wizardId, sagaId, StepCode.K8S_DEPLOY,
                    available ? "Namespace · Deployment 적용" : "클러스터 없음 — K8s 시뮬레이션",
                    80, WizardStatus.DEPLOYING);
            sleep(150);
            if (abort(wizardId)) {
                return;
            }
            K8sDeploymentRecord deploy = k8sDeployService.deployFromWizard(load(wizardId));
            sagaService.succeedStep(sagaId, StepCode.K8S_DEPLOY,
                    "Deploy " + deploy.getStatus() + " ns=" + deploy.getNamespaceName());

            // FINALIZE
            stepRun(wizardId, sagaId, StepCode.FINALIZE, "Service · Environment · Pipeline 확정",
                    95, WizardStatus.DEPLOYING);
            if (abort(wizardId)) {
                return;
            }
            complete(wizardId, repo, deploy);
            sagaService.succeedStep(sagaId, StepCode.FINALIZE, "Service ready");
            sagaService.complete(sagaId);
            applyProgress(wizardId, 100, "Completed", WizardStatus.COMPLETED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failSaga(wizardId, sagaId, null, "Interrupted");
        } catch (Exception e) {
            log.error("Provision failed for wizard {} saga {}", wizardId, sagaId, e);
            failSaga(wizardId, sagaId, null, e.getMessage() != null ? e.getMessage() : "Unknown error");
        }
    }

    private void stepRun(
            UUID wizardId,
            UUID sagaId,
            StepCode code,
            String message,
            int progress,
            WizardStatus status
    ) {
        sagaService.startStep(sagaId, code, message);
        applyProgress(wizardId, progress, message, status);
    }

    private void failSaga(UUID wizardId, UUID sagaId, StepCode failedCode, String reason) {
        if (failedCode != null) {
            try {
                sagaService.failStep(sagaId, failedCode, reason);
            } catch (Exception ignored) {
                // step may already be failed
            }
        }
        try {
            sagaService.compensate(sagaId, reason);
        } catch (Exception e) {
            log.warn("Compensation failed for saga {}: {}", sagaId, e.getMessage());
            try {
                sagaService.failWithoutCompensate(sagaId, reason);
            } catch (Exception ignored) {
            }
        }
        fail(wizardId, reason);
    }

    private boolean abort(UUID wizardId) {
        return isFailed(wizardId) || isCancelled(wizardId);
    }

    private WizardDtos.PreviewResponse resolvePreview(ServiceWizard wizard) {
        if (wizard == null) {
            throw new BusinessException(ErrorCode.WIZARD_NOT_FOUND);
        }
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
    protected void applyProgress(UUID wizardId, int progress, String message, WizardStatus status) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
        if (wizard == null || wizard.getStatus() == WizardStatus.CANCELLED) {
            return;
        }
        // COMPLETED 는 complete() 가 이미 설정 — 덮어쓰지 않음 단 최종 메시지
        if (wizard.getStatus() == WizardStatus.COMPLETED && status != WizardStatus.COMPLETED) {
            return;
        }
        if (status == WizardStatus.COMPLETED) {
            // complete() already set service; only touch progress message if needed
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

        try {
            var env = environmentService.ensureDefaultForService(service, wizard.getCreatedBy());
            wizard.appendLogPublic("Environment: " + env.getType() + " ns=" + env.getNamespaceName());
        } catch (Exception e) {
            log.warn("Failed to ensure default environment: {}", e.getMessage());
        }

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
