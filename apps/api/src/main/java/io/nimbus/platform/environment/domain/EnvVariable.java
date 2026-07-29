package io.nimbus.platform.environment.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "env_variables")
public class EnvVariable extends BaseEntity {

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "config_key", nullable = false, length = 128)
    private String key;

    @Column(name = "config_value", nullable = false, length = 500)
    private String value;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected EnvVariable() {
    }

    public static EnvVariable create(
            UUID environmentId,
            UUID serviceId,
            UUID workspaceId,
            String key,
            String value,
            UUID createdBy
    ) {
        EnvVariable v = new EnvVariable();
        v.environmentId = environmentId;
        v.serviceId = serviceId;
        v.workspaceId = workspaceId;
        v.key = key;
        v.value = value;
        v.createdBy = createdBy;
        return v;
    }

    public void updateValue(String value) {
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
