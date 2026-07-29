package io.nimbus.platform.provision.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provision_sagas")
public class ProvisionSaga extends BaseEntity {

    @Column(name = "wizard_id", nullable = false)
    private UUID wizardId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private Integer attempt = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SagaStatus status = SagaStatus.QUEUED;

    @Column(name = "current_step_code", length = 32)
    private String currentStepCode;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Lob
    @Column(name = "compensation_log")
    private String compensationLog;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected ProvisionSaga() {
    }

    public static ProvisionSaga create(
            UUID wizardId,
            UUID workspaceId,
            UUID projectId,
            int attempt,
            UUID createdBy
    ) {
        ProvisionSaga s = new ProvisionSaga();
        s.wizardId = wizardId;
        s.workspaceId = workspaceId;
        s.projectId = projectId;
        s.attempt = attempt;
        s.createdBy = createdBy;
        s.status = SagaStatus.QUEUED;
        s.compensationLog = "";
        return s;
    }

    public void markRunning() {
        this.status = SagaStatus.RUNNING;
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    public void markCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.currentStepCode = null;
    }

    public void markFailed(String reason) {
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.finishedAt = Instant.now();
    }

    public void markRollingBack() {
        this.status = SagaStatus.ROLLING_BACK;
    }

    public void markRolledBack(String reason) {
        this.status = SagaStatus.ROLLED_BACK;
        this.failureReason = reason;
        this.finishedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = SagaStatus.CANCELLED;
        this.finishedAt = Instant.now();
    }

    public void setCurrentStepCode(String code) {
        this.currentStepCode = code;
    }

    public void appendCompensation(String line) {
        String existing = this.compensationLog == null ? "" : this.compensationLog;
        this.compensationLog = existing + "[" + Instant.now() + "] " + line + "\n";
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public String getCurrentStepCode() {
        return currentStepCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCompensationLog() {
        return compensationLog;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
