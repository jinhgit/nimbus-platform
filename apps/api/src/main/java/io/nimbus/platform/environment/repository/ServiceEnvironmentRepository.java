package io.nimbus.platform.environment.repository;

import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceEnvironmentRepository extends JpaRepository<ServiceEnvironment, UUID> {

    Optional<ServiceEnvironment> findByIdAndDeletedAtIsNull(UUID id);

    List<ServiceEnvironment> findByServiceIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID serviceId);

    Optional<ServiceEnvironment> findByServiceIdAndTypeAndDeletedAtIsNull(UUID serviceId, EnvironmentType type);

    boolean existsByServiceIdAndTypeAndDeletedAtIsNull(UUID serviceId, EnvironmentType type);

    long countByServiceIdAndDeletedAtIsNull(UUID serviceId);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
