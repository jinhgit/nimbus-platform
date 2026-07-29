package io.nimbus.platform.wizard.domain;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.common.domain.BaseEntity;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "service_wizards")
public class ServiceWizard extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "service_name", nullable = false, length = 50)
    private String serviceName;

    @Column(name = "template_id")
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private RuntimeType runtime;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", length = 16)
    private EnvironmentType environmentType = EnvironmentType.DEV;

    @Column(length = 32)
    private String databaseType;

    @Column(length = 32)
    private String cacheType;

    @Column(name = "replica_count")
    private Integer replicaCount = 1;

    @Column
    private Boolean hpaEnabled = false;

    @Column(length = 64)
    private String cpu = "500m";

    @Column(length = 64)
    private String memory = "512Mi";

    @Column(length = 200)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WizardStatus status = WizardStatus.DRAFT;

    @Column(name = "current_step")
    private Integer currentStep = 1;

    @Column
    private Integer progress = 0;

    @Column(name = "progress_message", length = 200)
    private String progressMessage;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Lob
    private String recommendationJson;

    @Lob
    private String previewJson;

    @Lob
    private String logs;

    protected ServiceWizard() {
    }

    public static ServiceWizard create(
            UUID projectId,
            UUID workspaceId,
            String serviceName,
            UUID templateId,
            UUID createdBy
    ) {
        ServiceWizard w = new ServiceWizard();
        w.projectId = projectId;
        w.workspaceId = workspaceId;
        w.serviceName = serviceName;
        w.templateId = templateId;
        w.createdBy = createdBy;
        w.status = WizardStatus.DRAFT;
        w.currentStep = 1;
        w.progress = 0;
        w.logs = "";
        return w;
    }

    public void updateDraft(
            String serviceName,
            UUID templateId,
            RuntimeType runtime,
            EnvironmentType environmentType,
            String databaseType,
            String cacheType,
            Integer replicaCount,
            Boolean hpaEnabled,
            String cpu,
            String memory,
            String domain,
            Integer currentStep
    ) {
        if (serviceName != null && !serviceName.isBlank()) {
            this.serviceName = serviceName;
        }
        if (templateId != null) {
            this.templateId = templateId;
        }
        if (runtime != null) {
            this.runtime = runtime;
        }
        if (environmentType != null) {
            this.environmentType = environmentType;
        }
        if (databaseType != null) {
            this.databaseType = databaseType;
        }
        if (cacheType != null) {
            this.cacheType = cacheType;
        }
        if (replicaCount != null) {
            this.replicaCount = replicaCount;
        }
        if (hpaEnabled != null) {
            this.hpaEnabled = hpaEnabled;
        }
        if (cpu != null) {
            this.cpu = cpu;
        }
        if (memory != null) {
            this.memory = memory;
        }
        if (domain != null) {
            this.domain = domain;
        }
        if (currentStep != null) {
            this.currentStep = currentStep;
        }
    }

    public void applyRecommendation(
            RuntimeType runtime,
            String databaseType,
            String cacheType,
            Integer replicaCount,
            Boolean hpaEnabled,
            String recommendationJson
    ) {
        if (runtime != null) {
            this.runtime = runtime;
        }
        this.databaseType = databaseType;
        this.cacheType = cacheType;
        if (replicaCount != null) {
            this.replicaCount = replicaCount;
        }
        if (hpaEnabled != null) {
            this.hpaEnabled = hpaEnabled;
        }
        this.recommendationJson = recommendationJson;
    }

    public void setPreviewJson(String previewJson) {
        this.previewJson = previewJson;
    }

    public void startProvisioning() {
        this.status = WizardStatus.PROVISIONING;
        this.progress = 0;
        this.progressMessage = "Queued";
        appendLog("Provision job queued");
    }

    public void updateProgress(int progress, String message, WizardStatus status) {
        this.progress = progress;
        this.progressMessage = message;
        if (status != null) {
            this.status = status;
        }
        if (message != null) {
            appendLog(message + " (" + progress + "%)");
        }
    }

    public void complete(UUID serviceId) {
        this.serviceId = serviceId;
        this.status = WizardStatus.COMPLETED;
        this.progress = 100;
        this.progressMessage = "Completed";
        this.currentStep = 7;
        appendLog("Wizard completed");
    }

    public void fail(String reason) {
        this.status = WizardStatus.FAILED;
        this.progressMessage = reason;
        appendLog("FAILED: " + reason);
    }

    public void cancel() {
        this.status = WizardStatus.CANCELLED;
        this.progressMessage = "Cancelled";
        appendLog("Cancelled by user");
    }

    private void appendLog(String line) {
        String existing = this.logs == null ? "" : this.logs;
        this.logs = existing + "[" + java.time.Instant.now() + "] " + line + "\n";
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public RuntimeType getRuntime() {
        return runtime;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getCacheType() {
        return cacheType;
    }

    public Integer getReplicaCount() {
        return replicaCount;
    }

    public Boolean getHpaEnabled() {
        return hpaEnabled;
    }

    public String getCpu() {
        return cpu;
    }

    public String getMemory() {
        return memory;
    }

    public String getDomain() {
        return domain;
    }

    public WizardStatus getStatus() {
        return status;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getRecommendationJson() {
        return recommendationJson;
    }

    public String getPreviewJson() {
        return previewJson;
    }

    public String getLogs() {
        return logs;
    }
}
