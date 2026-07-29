package io.nimbus.platform.auth.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "github_id", unique = true)
    private String githubId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GlobalRole role = GlobalRole.DEVELOPER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "current_workspace_id")
    private UUID currentWorkspaceId;

    protected User() {
    }

    public static User createLocal(String email, String name) {
        User user = new User();
        user.email = email;
        user.name = name;
        user.githubId = "local:" + email;
        user.role = GlobalRole.DEVELOPER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public static User createFromGitHub(String githubId, String email, String name, String avatarUrl) {
        User user = new User();
        user.githubId = githubId;
        user.email = email;
        user.name = name;
        user.avatarUrl = avatarUrl;
        user.role = GlobalRole.DEVELOPER;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    public void markLogin() {
        this.lastLogin = Instant.now();
    }

    public void switchWorkspace(UUID workspaceId) {
        this.currentWorkspaceId = workspaceId;
    }

    public String getGithubId() {
        return githubId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public GlobalRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public UUID getCurrentWorkspaceId() {
        return currentWorkspaceId;
    }

    public void updateProfile(String name, String avatarUrl) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
    }
}
