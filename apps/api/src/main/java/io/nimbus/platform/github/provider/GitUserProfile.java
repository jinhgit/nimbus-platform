package io.nimbus.platform.github.provider;

public record GitUserProfile(
        String id,
        String login,
        String name,
        String avatarUrl,
        String htmlUrl
) {
}
