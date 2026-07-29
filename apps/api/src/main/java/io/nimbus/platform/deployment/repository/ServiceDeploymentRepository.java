package io.nimbus.platform.deployment.repository;

import io.nimbus.platform.deployment.domain.ServiceDeployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceDeploymentRepository extends JpaRepository<ServiceDeployment, UUID> {

    Optional<ServiceDeployment> findByIdAndDeletedAtIsNull(UUID id);

    List<ServiceDeployment> findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID serviceId);

    List<ServiceDeployment> findByEnvironmentIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID environmentId);

    long countByServiceIdAndDeletedAtIsNull(UUID serviceId);
}
