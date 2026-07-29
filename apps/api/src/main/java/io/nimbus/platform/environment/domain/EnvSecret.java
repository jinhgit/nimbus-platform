package io.nimbus.platform.environment.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Secret — valueEnc 에 AES 암호문만 저장. API 응답은 마스킹.
 */
@Entity
@Table(name = "env_secrets")
public class EnvSecret extends BaseEntity {

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "config_key", nullable = false, length = 128)
    private String key;

    @Column(name = "value_enc", nullable = false, length = 2000)
    private String valueEnc;

    /** 시크릿 로테이션 횟수 (JPA @Version 과 구분) */
    @Column(name = "rotation_version", nullable = false)
    private Integer rotationVersion = 1;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "last_synced_at")
    private java.time.Instant lastSyncedAt;

    @Column(name = "last_sync_status", length = 24)
    private String lastSyncStatus;

    @Column(name = "last_sync_message", length = 500)
    private String lastSyncMessage;

    protected EnvSecret() {
    }

    public static EnvSecret create(
            UUID environmentId,
            UUID serviceId,
            UUID workspaceId,
            String key,
            String valueEnc,
            UUID createdBy
    ) {
        EnvSecret s = new EnvSecret();
        s.environmentId = environmentId;
        s.serviceId = serviceId;
        s.workspaceId = workspaceId;
        s.key = key;
        s.valueEnc = valueEnc;
        s.rotationVersion = 1;
        s.createdBy = createdBy;
        return s;
    }

    public void rotate(String valueEnc) {
        this.valueEnc = valueEnc;
        this.rotationVersion = (this.rotationVersion == null ? 1 : this.rotationVersion) + 1;
    }

    public void markSynced(String status, String message) {
        this.lastSyncedAt = java.time.Instant.now();
        this.lastSyncStatus = status;
        this.lastSyncMessage = message != null && message.length() > 500
                ? message.substring(0, 500)
                : message;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getKey() {
        return key;
    }

    public String getValueEnc() {
        return valueEnc;
    }

    public Integer getRotationVersion() {
        return rotationVersion;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public java.time.Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getLastSyncStatus() {
        return lastSyncStatus;
    }

    public String getLastSyncMessage() {
        return lastSyncMessage;
    }
}
