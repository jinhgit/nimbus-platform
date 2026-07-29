package io.nimbus.platform.environment.repository;

import io.nimbus.platform.environment.domain.PromotionRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionRecordRepository extends JpaRepository<PromotionRecord, UUID> {

    List<PromotionRecord> findByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID serviceId);

    List<PromotionRecord> findByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
