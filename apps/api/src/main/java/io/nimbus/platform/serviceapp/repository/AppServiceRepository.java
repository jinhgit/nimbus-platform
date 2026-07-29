package io.nimbus.platform.serviceapp.repository;

import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppServiceRepository extends JpaRepository<AppService, UUID> {
    Optional<AppService> findByIdAndDeletedAtIsNull(UUID id);

    List<AppService> findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID projectId);

    List<AppService> findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID workspaceId);

    boolean existsByProjectIdAndNameAndDeletedAtIsNull(UUID projectId, String name);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    long countByWorkspaceIdAndStatusAndDeletedAtIsNull(UUID workspaceId, ServiceStatus status);

    long countByProjectIdAndDeletedAtIsNull(UUID projectId);
}
