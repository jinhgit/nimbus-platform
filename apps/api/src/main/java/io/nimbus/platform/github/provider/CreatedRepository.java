package io.nimbus.platform.github.provider;

public record CreatedRepository(
        String githubRepoId,
        String owner,
        String name,
        String htmlUrl,
        String cloneUrl,
        String defaultBranch,
        boolean privateRepo
) {
}
