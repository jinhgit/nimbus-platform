package io.nimbus.platform.deployment.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 배포 이력 — Wizard 완료 · Promote 등 플랫폼 이벤트의 고정 기록 (Sprint D).
 */
@Entity
@Table(name = "service_deployments")
public class ServiceDeployment extends BaseEntity {

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "environment_id")
    private UUID environmentId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", length = 16)
    private EnvironmentType environmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeploymentStatus status = DeploymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeploymentTrigger trigger = DeploymentTrigger.MANUAL;

    @Column(name = "version_label", length = 64)
    private String versionLabel;

    @Column(name = "image_tag", length = 128)
    private String imageTag;

    @Column(name = "namespace_name", length = 63)
    private String namespaceName;

    @Column(length = 500)
    private String message;

    @Column(name = "promotion_id")
    private UUID promotionId;

    @Column(name = "wizard_id")
    private UUID wizardId;

    @Column(name = "pipeline_id")
    private UUID pipelineId;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ServiceDeployment() {
    }

    public static ServiceDeployment create(
            UUID serviceId,
            UUID environmentId,
            UUID workspaceId,
            UUID projectId,
            EnvironmentType environmentType,
            DeploymentStatus status,
            DeploymentTrigger trigger,
            String versionLabel,
            String imageTag,
            String namespaceName,
            String message,
            UUID promotionId,
            UUID wizardId,
            UUID pipelineId,
            UUID triggeredBy
    ) {
        ServiceDeployment d = new ServiceDeployment();
        d.serviceId = serviceId;
        d.environmentId = environmentId;
        d.workspaceId = workspaceId;
        d.projectId = projectId;
        d.environmentType = environmentType;
        d.status = status != null ? status : DeploymentStatus.SUCCESS;
        d.trigger = trigger != null ? trigger : DeploymentTrigger.MANUAL;
        d.versionLabel = versionLabel;
        d.imageTag = imageTag;
        d.namespaceName = namespaceName;
        d.message = message;
        d.promotionId = promotionId;
        d.wizardId = wizardId;
        d.pipelineId = pipelineId;
        d.triggeredBy = triggeredBy;
        if (status == DeploymentStatus.SUCCESS
                || status == DeploymentStatus.FAILED
                || status == DeploymentStatus.SIMULATED
                || status == DeploymentStatus.ROLLED_BACK) {
            d.finishedAt = Instant.now();
        }
        return d;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public DeploymentTrigger getTrigger() {
        return trigger;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public String getMessage() {
        return message;
    }

    public UUID getPromotionId() {
        return promotionId;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public UUID getPipelineId() {
        return pipelineId;
    }

    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
