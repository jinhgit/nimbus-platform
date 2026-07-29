package io.nimbus.platform.wizard.service;

import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.domain.WizardStatus;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * MVP Provision 시뮬레이션 (시연 Progress Bar).
 * 실제 GitHub/k3d 연동은 다음 스프린트에서 Adapter 로 교체.
 */
@Component
public class WizardProvisionRunner {

    private static final Logger log = LoggerFactory.getLogger(WizardProvisionRunner.class);

    private final ServiceWizardRepository wizardRepository;
    private final AppServiceRepository appServiceRepository;

    public WizardProvisionRunner(
            ServiceWizardRepository wizardRepository,
            AppServiceRepository appServiceRepository
    ) {
        this.wizardRepository = wizardRepository;
        this.appServiceRepository = appServiceRepository;
    }

    @Async
    public void runAsync(UUID wizardId) {
        try {
            List<Step> steps = List.of(
                    new Step("Repository 생성", 15, WizardStatus.PROVISIONING),
                    new Step("GitHub Actions 생성", 30, WizardStatus.PROVISIONING),
                    new Step("Helm Chart 생성", 45, WizardStatus.PROVISIONING),
                    new Step("Terraform 파일 생성", 60, WizardStatus.PROVISIONING),
                    new Step("ArgoCD Manifest 생성", 75, WizardStatus.PROVISIONING),
                    new Step("Deploy (local simulation)", 90, WizardStatus.DEPLOYING),
                    new Step("Verify Health", 100, WizardStatus.DEPLOYING)
            );

            for (Step step : steps) {
                Thread.sleep(700);
                applyStep(wizardId, step);
            }
            complete(wizardId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(wizardId, "Interrupted");
        } catch (Exception e) {
            log.error("Provision failed for wizard {}", wizardId, e);
            fail(wizardId, e.getMessage());
        }
    }

    @Transactional
    protected void applyStep(UUID wizardId, Step step) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId).orElse(null);
        if (wizard == null) {
            return;
        }
        if (wizard.getStatus() == WizardStatus.CANCELLED) {
            return;
        }
        wizard.updateProgress(step.progress(), step.message(), step.status());
        wizardRepository.save(wizard);
    }

    @Transactional
    protected void complete(UUID wizardId) {
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
        service.markReady();
        service = appServiceRepository.save(service);
        wizard.complete(service.getId());
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

    private record Step(String message, int progress, WizardStatus status) {
    }
}
