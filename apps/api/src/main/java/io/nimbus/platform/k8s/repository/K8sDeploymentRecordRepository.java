package io.nimbus.platform.k8s.repository;

import io.nimbus.platform.k8s.domain.K8sDeploymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface K8sDeploymentRecordRepository extends JpaRepository<K8sDeploymentRecord, UUID> {
    List<K8sDeploymentRecord> findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workspaceId);

    Optional<K8sDeploymentRecord> findByWizardIdAndDeletedAtIsNull(UUID wizardId);

    Optional<K8sDeploymentRecord> findByServiceIdAndDeletedAtIsNull(UUID serviceId);

    List<K8sDeploymentRecord> findByDeletedAtIsNullOrderByCreatedAtDesc();
}
