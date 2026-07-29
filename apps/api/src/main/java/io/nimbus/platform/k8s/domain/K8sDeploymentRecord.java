package io.nimbus.platform.k8s.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "k8s_deployments")
public class K8sDeploymentRecord extends BaseEntity {

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "wizard_id")
    private UUID wizardId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(nullable = false, length = 63)
    private String namespaceName;

    @Column(name = "deployment_name", nullable = false, length = 63)
    private String deploymentName;

    @Column(name = "service_name", length = 63)
    private String k8sServiceName;

    @Column(length = 200)
    private String image;

    @Column
    private Integer replicas = 1;

    @Column(name = "ready_replicas")
    private Integer readyReplicas = 0;

    @Column(name = "cluster_context", length = 200)
    private String clusterContext;

    @Column(name = "cluster_type", length = 32)
    private String clusterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeployStatus status = DeployStatus.PENDING;

    @Column(length = 500)
    private String message;

    protected K8sDeploymentRecord() {
    }

    public static K8sDeploymentRecord create(
            UUID wizardId,
            UUID projectId,
            UUID workspaceId,
            String namespaceName,
            String deploymentName,
            String image,
            int replicas,
            String clusterContext,
            String clusterType
    ) {
        K8sDeploymentRecord r = new K8sDeploymentRecord();
        r.wizardId = wizardId;
        r.projectId = projectId;
        r.workspaceId = workspaceId;
        r.namespaceName = namespaceName;
        r.deploymentName = deploymentName;
        r.k8sServiceName = deploymentName;
        r.image = image;
        r.replicas = replicas;
        r.clusterContext = clusterContext;
        r.clusterType = clusterType;
        r.status = DeployStatus.DEPLOYING;
        return r;
    }

    public void bindService(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public void markRunning(int readyReplicas) {
        this.readyReplicas = readyReplicas;
        this.status = DeployStatus.RUNNING;
        this.message = "Pods ready";
    }

    public void markSimulated() {
        this.status = DeployStatus.SIMULATED;
        this.message = "Local cluster unavailable — simulated deploy";
        this.readyReplicas = this.replicas;
    }

    public void markFailed(String message) {
        this.status = DeployStatus.FAILED;
        this.message = message;
    }

    public void updateReady(int readyReplicas, DeployStatus status) {
        this.readyReplicas = readyReplicas;
        this.status = status;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getWizardId() {
        return wizardId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getK8sServiceName() {
        return k8sServiceName;
    }

    public String getImage() {
        return image;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public Integer getReadyReplicas() {
        return readyReplicas;
    }

    public String getClusterContext() {
        return clusterContext;
    }

    public String getClusterType() {
        return clusterType;
    }

    public DeployStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
