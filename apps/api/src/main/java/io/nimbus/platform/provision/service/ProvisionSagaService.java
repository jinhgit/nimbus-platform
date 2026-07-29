package io.nimbus.platform.provision.service;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.domain.ProvisionSagaStep;
import io.nimbus.platform.provision.domain.SagaStatus;
import io.nimbus.platform.provision.domain.StepCode;
import io.nimbus.platform.provision.domain.StepStatus;
import io.nimbus.platform.provision.dto.ProvisionDtos;
import io.nimbus.platform.provision.repository.ProvisionSagaRepository;
import io.nimbus.platform.provision.repository.ProvisionSagaStepRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProvisionSagaService {

    private final ProvisionSagaRepository sagaRepository;
    private final ProvisionSagaStepRepository stepRepository;
    private final ServiceWizardRepository wizardRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;

    public ProvisionSagaService(
            ProvisionSagaRepository sagaRepository,
            ProvisionSagaStepRepository stepRepository,
            ServiceWizardRepository wizardRepository,
            WorkspaceBootstrapService workspaceBootstrapService
    ) {
        this.sagaRepository = sagaRepository;
        this.stepRepository = stepRepository;
        this.wizardRepository = wizardRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProvisionSaga createForWizard(ServiceWizard wizard) {
        int nextAttempt = sagaRepository.maxAttempt(wizard.getId()) + 1;
        ProvisionSaga saga = sagaRepository.save(ProvisionSaga.create(
                wizard.getId(),
                wizard.getWorkspaceId(),
                wizard.getProjectId(),
                nextAttempt,
                wizard.getCreatedBy()
        ));
        List<ProvisionSagaStep> steps = new ArrayList<>();
        for (StepCode code : StepCode.values()) {
            steps.add(ProvisionSagaStep.waiting(saga.getId(), wizard.getId(), code));
        }
        stepRepository.saveAll(steps);
        return saga;
    }

    @Transactional
    public void markRunning(UUID sagaId) {
        ProvisionSaga saga = requireSaga(sagaId);
        saga.markRunning();
        sagaRepository.save(saga);
    }

    @Transactional
    public void startStep(UUID sagaId, StepCode code, String message) {
        ProvisionSaga saga = requireSaga(sagaId);
        saga.setCurrentStepCode(code.name());
        sagaRepository.save(saga);
        ProvisionSagaStep step = requireStep(sagaId, code);
        step.markRunning(message);
        stepRepository.save(step);
    }

    @Transactional
    public void succeedStep(UUID sagaId, StepCode code, String message) {
        ProvisionSagaStep step = requireStep(sagaId, code);
        step.markSuccess(message);
        stepRepository.save(step);
    }

    @Transactional
    public void failStep(UUID sagaId, StepCode code, String message) {
        ProvisionSagaStep step = requireStep(sagaId, code);
        step.markFailed(message);
        stepRepository.save(step);
    }

    @Transactional
    public void skipStep(UUID sagaId, StepCode code, String message) {
        ProvisionSagaStep step = requireStep(sagaId, code);
        step.markSkipped(message);
        stepRepository.save(step);
    }

    /**
     * 성공한 step 역순 보상 로그 기록 (실 리소스 삭제는 free-only 범위에서 시뮬레이션).
     */
    @Transactional
    public void compensate(UUID sagaId, String failureReason) {
        ProvisionSaga saga = requireSaga(sagaId);
        saga.markRollingBack();
        sagaRepository.save(saga);

        List<ProvisionSagaStep> successSteps = stepRepository
                .findBySagaIdAndStatusAndDeletedAtIsNullOrderByStepOrderDesc(sagaId, StepStatus.SUCCESS);

        for (ProvisionSagaStep step : successSteps) {
            String msg = compensationMessage(step.getStepCode());
            step.markRollback(msg);
            stepRepository.save(step);
            saga.appendCompensation("ROLLBACK " + step.getStepCode() + ": " + msg);
            step.markRolledBack(msg);
            stepRepository.save(step);
        }

        // RUNNING 상태 step 실패 처리
        stepRepository.findBySagaIdAndDeletedAtIsNullOrderByStepOrderAsc(sagaId).stream()
                .filter(s -> s.getStatus() == StepStatus.RUNNING)
                .forEach(s -> {
                    s.markFailed(failureReason);
                    stepRepository.save(s);
                });

        // WAITING steps skip
        stepRepository.findBySagaIdAndDeletedAtIsNullOrderByStepOrderAsc(sagaId).stream()
                .filter(s -> s.getStatus() == StepStatus.WAITING)
                .forEach(s -> {
                    s.markSkipped("Skipped after failure");
                    stepRepository.save(s);
                });

        saga.markRolledBack(failureReason);
        sagaRepository.save(saga);
    }

    @Transactional
    public void complete(UUID sagaId) {
        ProvisionSaga saga = requireSaga(sagaId);
        saga.markCompleted();
        sagaRepository.save(saga);
    }

    @Transactional
    public void failWithoutCompensate(UUID sagaId, String reason) {
        ProvisionSaga saga = requireSaga(sagaId);
        saga.markFailed(reason);
        sagaRepository.save(saga);
    }

    @Transactional(readOnly = true)
    public ProvisionDtos.SagaResponse getLatestForWizard(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WIZARD_NOT_FOUND));
        workspaceBootstrapService.requireMember(wizard.getWorkspaceId(), principal.userId());
        ProvisionSaga saga = sagaRepository.findFirstByWizardIdAndDeletedAtIsNullOrderByAttemptDesc(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVISION_SAGA_NOT_FOUND));
        return toResponse(saga);
    }

    @Transactional(readOnly = true)
    public List<ProvisionDtos.SagaResponse> listForWizard(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WIZARD_NOT_FOUND));
        workspaceBootstrapService.requireMember(wizard.getWorkspaceId(), principal.userId());
        return sagaRepository.findByWizardIdAndDeletedAtIsNullOrderByAttemptDesc(wizardId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProvisionDtos.SagaResponse toResponse(ProvisionSaga saga) {
        List<ProvisionDtos.SagaStepResponse> steps = stepRepository
                .findBySagaIdAndDeletedAtIsNullOrderByStepOrderAsc(saga.getId())
                .stream()
                .sorted(Comparator.comparing(ProvisionSagaStep::getStepOrder))
                .map(s -> new ProvisionDtos.SagaStepResponse(
                        s.getId(),
                        s.getStepCode(),
                        s.getName(),
                        s.getStepOrder(),
                        s.getStatus(),
                        s.getMessage(),
                        s.getCompensationMessage(),
                        s.getStartedAt(),
                        s.getFinishedAt()
                ))
                .toList();
        return new ProvisionDtos.SagaResponse(
                saga.getId(),
                saga.getWizardId(),
                saga.getAttempt(),
                saga.getStatus(),
                saga.getCurrentStepCode(),
                saga.getFailureReason(),
                saga.getCompensationLog(),
                saga.getStartedAt(),
                saga.getFinishedAt(),
                steps
        );
    }

    private ProvisionSaga requireSaga(UUID sagaId) {
        return sagaRepository.findByIdAndDeletedAtIsNull(sagaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVISION_SAGA_NOT_FOUND));
    }

    private ProvisionSagaStep requireStep(UUID sagaId, StepCode code) {
        return stepRepository.findBySagaIdAndStepCodeAndDeletedAtIsNull(sagaId, code)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROVISION_STEP_NOT_FOUND, code.name()));
    }

    private static String compensationMessage(StepCode code) {
        return switch (code) {
            case SCM_PREPARE -> "Release SCM session (noop)";
            case SCM_REPO -> "Mark repo for cleanup / soft-orphan (sim if not connected)";
            case SCM_MANIFESTS -> "Discard uncommitted manifest batch (sim)";
            case K8S_PREPARE -> "Drop pending namespace request";
            case K8S_DEPLOY -> "Scale to 0 / leave sim deployment record";
            case FINALIZE -> "Do not create service entity (already failed before or reverse)";
        };
    }

    public static List<StepCode> orderedCodes() {
        return Arrays.asList(StepCode.values());
    }
}
