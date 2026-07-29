package io.nimbus.platform.environment.repository;

import io.nimbus.platform.environment.domain.EnvVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvVariableRepository extends JpaRepository<EnvVariable, UUID> {

    Optional<EnvVariable> findByIdAndDeletedAtIsNull(UUID id);

    List<EnvVariable> findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(UUID environmentId);

    Optional<EnvVariable> findByEnvironmentIdAndKeyAndDeletedAtIsNull(UUID environmentId, String key);

    boolean existsByEnvironmentIdAndKeyAndDeletedAtIsNull(UUID environmentId, String key);
}
