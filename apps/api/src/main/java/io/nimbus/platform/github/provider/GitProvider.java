package io.nimbus.platform.github.provider;

/**
 * SCM 추상화. 비즈니스 로직은 GitHub REST를 직접 호출하지 않는다.
 */
public interface GitProvider {

    String providerName();

    GitUserProfile validateToken(String accessToken);

    CreatedRepository createRepository(CreateRepositoryCommand command);

    void putFile(String accessToken, String owner, String repo, String path, String content, String message);

    RateLimitStatus rateLimit(String accessToken);

    record RateLimitStatus(int remaining, int limit, long resetEpochSeconds) {
    }
}
