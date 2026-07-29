package io.nimbus.platform.auth.security;

import java.time.Duration;
import java.util.Optional;

/**
 * OAuth state / refresh token 저장.
 * free-only 로컬: 인메모리 구현. Redis 연동은 이후 확장.
 */
public interface TokenStore {
    void put(String key, String value, Duration ttl);

    Optional<String> get(String key);

    void delete(String key);
}
