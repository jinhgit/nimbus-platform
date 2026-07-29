package io.nimbus.platform.environment.repository;

import io.nimbus.platform.environment.domain.EnvSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvSecretRepository extends JpaRepository<EnvSecret, UUID> {

    Optional<EnvSecret> findByIdAndDeletedAtIsNull(UUID id);

    List<EnvSecret> findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(UUID environmentId);

    Optional<EnvSecret> findByEnvironmentIdAndKeyAndDeletedAtIsNull(UUID environmentId, String key);

    boolean existsByEnvironmentIdAndKeyAndDeletedAtIsNull(UUID environmentId, String key);
}
