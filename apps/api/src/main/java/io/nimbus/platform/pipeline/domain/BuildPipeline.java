package io.nimbus.platform.pipeline.domain;

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
@Table(name = "build_pipelines")
public class BuildPipeline extends BaseEntity {

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "wizard_id")
    private UUID wizardId;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 100)
    private String name = "docker-build";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PipelineStatus status = PipelineStatus.QUEUED;

    @Column
    private Integer progress = 0;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "image_tag", length = 200)
    private String imageTag;

    @Column(name = "dockerfile_path", length = 200)
    private String dockerfilePath = "Dockerfile";

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Lob
    private String logs = "";

    protected BuildPipeline() {
    }

    public static BuildPipeline create(
            UUID serviceId,
            UUID projectId,
            UUID workspaceId,
            UUID wizardId,
            String serviceName,
            UUID triggeredBy
    ) {
        BuildPipeline p = new BuildPipeline();
        p.serviceId = serviceId;
        p.projectId = projectId;
        p.workspaceId = workspaceId;
        p.wizardId = wizardId;
        p.serviceName = serviceName;
        p.triggeredBy = triggeredBy;
        p.status = PipelineStatus.QUEUED;
        p.progress = 0;
        p.currentStep = "Queued";
        p.imageTag = "nimbus/" + serviceName + ":1.0.0";
        p.logs = "";
        return p;
    }

    public void start() {
        this.status = PipelineStatus.RUNNING;
        this.startedAt = Instant.now();
        this.progress = 1;
        appendLog("Pipeline started");
    }

    public void updateStep(String step, int progress) {
        this.currentStep = step;
        this.progress = progress;
        this.status = PipelineStatus.RUNNING;
        appendLog(step + " (" + progress + "%)");
    }

    public void succeed(String imageTag) {
        this.status = PipelineStatus.SUCCESS;
        this.progress = 100;
        this.currentStep = "Success";
        this.imageTag = imageTag;
        this.finishedAt = Instant.now();
        appendLog("Pipeline SUCCESS image=" + imageTag);
    }

    public void fail(String reason) {
        this.status = PipelineStatus.FAILED;
        this.currentStep = "Failed";
        this.finishedAt = Instant.now();
        appendLog("FAILED: " + reason);
    }

    private void appendLog(String line) {
        String existing = this.logs == null ? "" : this.logs;
        this.logs = existing + "[" + Instant.now() + "] " + line + "\n";
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getName() {
        return name;
    }

    public PipelineStatus getStatus() {
        return status;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getDockerfilePath() {
        return dockerfilePath;
    }

    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getLogs() {
        return logs;
    }
}
