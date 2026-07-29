package io.nimbus.platform.github.repository;

import io.nimbus.platform.github.domain.GitRepositoryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GitRepositoryRecordRepository extends JpaRepository<GitRepositoryRecord, UUID> {
    List<GitRepositoryRecord> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<GitRepositoryRecord> findByWizardIdAndDeletedAtIsNull(UUID wizardId);

    Optional<GitRepositoryRecord> findByServiceIdAndDeletedAtIsNull(UUID serviceId);

    boolean existsByOwnerAndRepoNameAndDeletedAtIsNull(String owner, String repoName);
}
