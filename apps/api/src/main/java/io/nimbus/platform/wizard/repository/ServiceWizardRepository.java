package io.nimbus.platform.wizard.repository;

import io.nimbus.platform.wizard.domain.ServiceWizard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceWizardRepository extends JpaRepository<ServiceWizard, UUID> {
    Optional<ServiceWizard> findByIdAndDeletedAtIsNull(UUID id);

    List<ServiceWizard> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID projectId);

    List<ServiceWizard> findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDesc(UUID createdBy);
}
