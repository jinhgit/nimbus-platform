package io.nimbus.platform.provision.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provision_saga_steps")
public class ProvisionSagaStep extends BaseEntity {

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "wizard_id", nullable = false)
    private UUID wizardId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_code", nullable = false, length = 32)
    private StepCode stepCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StepStatus status = StepStatus.WAITING;

    @Column(length = 500)
    private String message;

    @Column(name = "compensation_message", length = 500)
    private String compensationMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ProvisionSagaStep() {
    }

    public static ProvisionSagaStep waiting(UUID sagaId, UUID wizardId, StepCode code) {
        ProvisionSagaStep s = new ProvisionSagaStep();
        s.sagaId = sagaId;
        s.wizardId = wizardId;
        s.stepOrder = code.getOrder();
        s.stepCode = code;
        s.name = code.getDefaultName();
        s.status = StepStatus.WAITING;
        return s;
    }

    public void markRunning(String message) {
        this.status = StepStatus.RUNNING;
        this.message = message;
        this.startedAt = Instant.now();
    }

    public void markSuccess(String message) {
        this.status = StepStatus.SUCCESS;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = StepStatus.FAILED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public void markSkipped(String message) {
        this.status = StepStatus.SKIPPED;
        this.message = message;
        this.finishedAt = Instant.now();
    }

    public void markRollback(String compensationMessage) {
        this.status = StepStatus.ROLLBACK;
        this.compensationMessage = compensationMessage;
    }

    public void markRolledBack(String compensationMessage) {
        this.status = StepStatus.ROLLED_BACK;
        this.compensationMessage = compensationMessage;
        this.finishedAt = Instant.now();
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public StepCode getStepCode() {
        return stepCode;
    }

    public String getName() {
        return name;
    }

    public StepStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getCompensationMessage() {
        return compensationMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
