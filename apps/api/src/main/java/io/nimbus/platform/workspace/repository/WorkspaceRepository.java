package io.nimbus.platform.workspace.repository;

import io.nimbus.platform.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsBySlugAndDeletedAtIsNull(String slug);

    Optional<Workspace> findByIdAndDeletedAtIsNull(UUID id);
}
