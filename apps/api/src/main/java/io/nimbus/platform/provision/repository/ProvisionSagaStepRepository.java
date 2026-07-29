package io.nimbus.platform.provision.repository;

import io.nimbus.platform.provision.domain.ProvisionSagaStep;
import io.nimbus.platform.provision.domain.StepCode;
import io.nimbus.platform.provision.domain.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisionSagaStepRepository extends JpaRepository<ProvisionSagaStep, UUID> {

    List<ProvisionSagaStep> findBySagaIdAndDeletedAtIsNullOrderByStepOrderAsc(UUID sagaId);

    Optional<ProvisionSagaStep> findBySagaIdAndStepCodeAndDeletedAtIsNull(UUID sagaId, StepCode stepCode);

    List<ProvisionSagaStep> findBySagaIdAndStatusAndDeletedAtIsNullOrderByStepOrderDesc(
            UUID sagaId, StepStatus status
    );
}
