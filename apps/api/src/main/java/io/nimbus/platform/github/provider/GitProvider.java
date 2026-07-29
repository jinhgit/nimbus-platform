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

    /**
     * Actions secret public key. null if unavailable.
     */
    RepoPublicKey fetchActionsPublicKey(String accessToken, String owner, String repo);

    /**
     * Upsert GitHub Actions repository secret (encrypted_value + key_id).
     * Thin: if encryption unsupported, implementations may throw or no-op.
     */
    void putActionsSecret(
            String accessToken,
            String owner,
            String repo,
            String secretName,
            String encryptedValue,
            String keyId
    );

    /**
     * Thin pull request create. Implementations may throw if branches missing.
     */
    CreatedPullRequest createPullRequest(
            String accessToken,
            String owner,
            String repo,
            String title,
            String body,
            String head,
            String base
    );

    /**
     * Thin: recent GitHub Actions workflow runs (empty list if none).
     */
    java.util.List<WorkflowRun> listWorkflowRuns(
            String accessToken,
            String owner,
            String repo,
            int perPage
    );

    record RateLimitStatus(int remaining, int limit, long resetEpochSeconds) {
    }

    record RepoPublicKey(String keyId, String keyBase64) {
    }

    record CreatedPullRequest(int number, String htmlUrl, String state) {
    }

    record WorkflowRun(
            long id,
            String name,
            String status,
            String conclusion,
            String htmlUrl,
            String headBranch,
            String event,
            String createdAt,
            String updatedAt
    ) {
    }
}
