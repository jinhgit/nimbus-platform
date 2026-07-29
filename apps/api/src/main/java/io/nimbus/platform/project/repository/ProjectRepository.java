package io.nimbus.platform.project.repository;

import io.nimbus.platform.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByWorkspaceIdAndNameAndDeletedAtIsNull(UUID workspaceId, String name);

    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    List<Project> findByWorkspaceIdAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID workspaceId);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
