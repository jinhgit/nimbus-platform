package io.nimbus.platform.workspace.repository;

import io.nimbus.platform.workspace.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    Optional<Team> findByIdAndDeletedAtIsNull(UUID id);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
