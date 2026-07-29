package io.nimbus.platform.incident.repository;

import io.nimbus.platform.incident.domain.Incident;
import io.nimbus.platform.incident.domain.IncidentSource;
import io.nimbus.platform.incident.domain.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIdAndDeletedAtIsNull(UUID id);

    List<Incident> findByWorkspaceIdAndDeletedAtIsNullOrderByOpenedAtDesc(UUID workspaceId);

    List<Incident> findByWorkspaceIdAndStatusAndDeletedAtIsNullOrderByOpenedAtDesc(
            UUID workspaceId, IncidentStatus status
    );

    long countByWorkspaceIdAndStatusAndDeletedAtIsNull(UUID workspaceId, IncidentStatus status);

    boolean existsBySourceTypeAndSourceIdAndStatusNotAndDeletedAtIsNull(
            IncidentSource sourceType, UUID sourceId, IncidentStatus resolved
    );

    Optional<Incident> findFirstBySourceTypeAndSourceIdAndDeletedAtIsNullOrderByOpenedAtDesc(
            IncidentSource sourceType, UUID sourceId
    );
}
