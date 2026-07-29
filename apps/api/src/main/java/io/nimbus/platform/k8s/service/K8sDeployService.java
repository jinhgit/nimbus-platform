package io.nimbus.platform.k8s.service;

import io.nimbus.platform.k8s.client.LocalKubernetesGateway;
import io.nimbus.platform.k8s.config.KubernetesProperties;
import io.nimbus.platform.k8s.domain.DeployStatus;
import io.nimbus.platform.k8s.domain.K8sDeploymentRecord;
import io.nimbus.platform.k8s.dto.K8sDtos;
import io.nimbus.platform.k8s.repository.K8sDeploymentRecordRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class K8sDeployService {

    private final LocalKubernetesGateway gateway;
    private final K8sDeploymentRecordRepository deploymentRepository;
    private final KubernetesProperties properties;

    public K8sDeployService(
            LocalKubernetesGateway gateway,
            K8sDeploymentRecordRepository deploymentRepository,
            KubernetesProperties properties
    ) {
        this.gateway = gateway;
        this.deploymentRepository = deploymentRepository;
        this.properties = properties;
    }

    public K8sDtos.ClusterStatusResponse clusterStatus() {
        return gateway.status();
    }

    public void refreshConnection() {
        gateway.reconnect();
    }

    public boolean isClusterAvailable() {
        return gateway.isAvailable();
    }

    /**
     * Wizard 기반 로컬 클러스터 배포.
     * 클러스터 없으면 SIMULATED 레코드 반환 (실패로 전체 Wizard를 깨지 않음).
     */
    @Transactional
    public K8sDeploymentRecord deployFromWizard(ServiceWizard wizard) {
        int replicas = wizard.getReplicaCount() != null ? wizard.getReplicaCount() : 1;
        String env = wizard.getEnvironmentType() != null ? wizard.getEnvironmentType().name() : "DEV";
        String image = properties.getDemoImage();

        if (!gateway.isAvailable()) {
            K8sDeploymentRecord sim = K8sDeploymentRecord.create(
                    wizard.getId(),
                    wizard.getProjectId(),
                    wizard.getWorkspaceId(),
                    LocalKubernetesGateway.sanitizeK8sName(wizard.getServiceName() + "-" + env.toLowerCase()),
                    LocalKubernetesGateway.sanitizeK8sName(wizard.getServiceName()),
                    image,
                    replicas,
                    null,
                    "none"
            );
            sim.markSimulated();
            return deploymentRepository.save(sim);
        }

        K8sDtos.DeployResult result = gateway.deploy(
                wizard.getServiceName(),
                env,
                replicas,
                image
        );

        K8sDeploymentRecord record = K8sDeploymentRecord.create(
                wizard.getId(),
                wizard.getProjectId(),
                wizard.getWorkspaceId(),
                result.namespace(),
                result.deployment(),
                result.image(),
                result.replicas(),
                result.context(),
                result.clusterType()
        );

        if (result.status() == DeployStatus.RUNNING) {
            record.markRunning(result.readyReplicas());
        } else if (result.status() == DeployStatus.FAILED) {
            record.markFailed(result.message());
        } else {
            record.updateReady(result.readyReplicas(), result.status());
        }
        return deploymentRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<K8sDtos.DeploymentResponse> listDeployments(UUID workspaceId) {
        List<K8sDeploymentRecord> records = workspaceId != null
                ? deploymentRepository.findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId)
                : deploymentRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        return records.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public K8sDtos.DeploymentResponse getByService(UUID serviceId) {
        return deploymentRepository.findByServiceIdAndDeletedAtIsNull(serviceId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public void bindService(UUID wizardId, UUID serviceId) {
        deploymentRepository.findByWizardIdAndDeletedAtIsNull(wizardId).ifPresent(r -> {
            r.bindService(serviceId);
            deploymentRepository.save(r);
        });
    }

    private K8sDtos.DeploymentResponse toResponse(K8sDeploymentRecord r) {
        List<K8sDtos.PodSummary> pods = List.of();
        if (r.getStatus() != DeployStatus.SIMULATED && gateway.isAvailable()) {
            pods = gateway.listPods(r.getNamespaceName(), r.getDeploymentName());
            int ready = gateway.countReadyReplicas(r.getNamespaceName(), r.getDeploymentName());
            // live refresh without always persisting
            return new K8sDtos.DeploymentResponse(
                    r.getId(), r.getServiceId(), r.getWizardId(),
                    r.getNamespaceName(), r.getDeploymentName(), r.getImage(),
                    r.getReplicas(), ready, r.getClusterContext(), r.getClusterType(),
                    ready >= (r.getReplicas() != null ? r.getReplicas() : 1)
                            ? DeployStatus.RUNNING
                            : r.getStatus(),
                    r.getMessage(), r.getCreatedAt(), pods
            );
        }
        return new K8sDtos.DeploymentResponse(
                r.getId(), r.getServiceId(), r.getWizardId(),
                r.getNamespaceName(), r.getDeploymentName(), r.getImage(),
                r.getReplicas(), r.getReadyReplicas(), r.getClusterContext(), r.getClusterType(),
                r.getStatus(), r.getMessage(), r.getCreatedAt(), pods
        );
    }
}
