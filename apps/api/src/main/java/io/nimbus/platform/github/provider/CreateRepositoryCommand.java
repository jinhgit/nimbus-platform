package io.nimbus.platform.github.provider;

import java.util.List;

public record CreateRepositoryCommand(
        String accessToken,
        String owner,
        String repoName,
        String description,
        boolean isPrivate,
        List<RepoFile> files
) {
}
