package io.nimbus.platform.auth.repository;

import io.nimbus.platform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByGithubIdAndDeletedAtIsNull(String githubId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
