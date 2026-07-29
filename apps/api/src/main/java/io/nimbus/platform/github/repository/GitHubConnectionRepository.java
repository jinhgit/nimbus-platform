package io.nimbus.platform.github.repository;

import io.nimbus.platform.github.domain.ConnectionStatus;
import io.nimbus.platform.github.domain.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {
    Optional<GitHubConnection> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<GitHubConnection> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, ConnectionStatus status);
}
