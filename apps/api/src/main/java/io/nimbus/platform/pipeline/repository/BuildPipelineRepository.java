package io.nimbus.platform.pipeline.repository;

import io.nimbus.platform.pipeline.domain.BuildPipeline;
import io.nimbus.platform.pipeline.domain.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildPipelineRepository extends JpaRepository<BuildPipeline, UUID> {
    Optional<BuildPipeline> findByIdAndDeletedAtIsNull(UUID id);

    List<BuildPipeline> findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workspaceId);

    List<BuildPipeline> findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID serviceId);

    long countByWorkspaceIdAndStatusAndDeletedAtIsNull(UUID workspaceId, PipelineStatus status);
}
