package io.nimbus.platform.provision.repository;

import io.nimbus.platform.provision.domain.ProvisionSaga;
import io.nimbus.platform.provision.domain.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisionSagaRepository extends JpaRepository<ProvisionSaga, UUID> {

    Optional<ProvisionSaga> findByIdAndDeletedAtIsNull(UUID id);

    List<ProvisionSaga> findByWizardIdAndDeletedAtIsNullOrderByAttemptDesc(UUID wizardId);

    Optional<ProvisionSaga> findFirstByWizardIdAndDeletedAtIsNullOrderByAttemptDesc(UUID wizardId);

    @Query("select coalesce(max(s.attempt), 0) from ProvisionSaga s where s.wizardId = :wizardId and s.deletedAt is null")
    int maxAttempt(@Param("wizardId") UUID wizardId);

    long countByWizardIdAndStatusAndDeletedAtIsNull(UUID wizardId, SagaStatus status);
}
