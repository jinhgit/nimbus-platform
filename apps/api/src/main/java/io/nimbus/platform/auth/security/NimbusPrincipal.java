package io.nimbus.platform.auth.security;

import io.nimbus.platform.auth.domain.GlobalRole;

import java.util.UUID;

public record NimbusPrincipal(
        UUID userId,
        String email,
        String name,
        GlobalRole role,
        UUID workspaceId
) {
}
