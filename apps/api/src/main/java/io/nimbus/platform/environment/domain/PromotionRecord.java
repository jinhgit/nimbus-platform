package io.nimbus.platform.environment.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Promote 이력 (Sprint B 최소: 상태 전이 + 복사 결과 기록).
 */
@Entity
@Table(name = "environment_promotions")
public class PromotionRecord extends BaseEntity {

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "source_environment_id", nullable = false)
    private UUID sourceEnvironmentId;

    @Column(name = "target_environment_id")
    private UUID targetEnvironmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private EnvironmentType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private EnvironmentType targetType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PromotionStatus status = PromotionStatus.COMPLETED;

    @Column(name = "variables_copied")
    private Integer variablesCopied = 0;

    @Column(name = "secrets_copied")
    private Integer secretsCopied = 0;

    @Column(length = 500)
    private String message;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected PromotionRecord() {
    }

    public static PromotionRecord create(
            UUID serviceId,
            UUID workspaceId,
            UUID sourceEnvironmentId,
            UUID targetEnvironmentId,
            EnvironmentType sourceType,
            EnvironmentType targetType,
            int variablesCopied,
            int secretsCopied,
            String message,
            UUID requestedBy
    ) {
        PromotionRecord r = new PromotionRecord();
        r.serviceId = serviceId;
        r.workspaceId = workspaceId;
        r.sourceEnvironmentId = sourceEnvironmentId;
        r.targetEnvironmentId = targetEnvironmentId;
        r.sourceType = sourceType;
        r.targetType = targetType;
        r.variablesCopied = variablesCopied;
        r.secretsCopied = secretsCopied;
        r.message = message;
        r.requestedBy = requestedBy;
        r.status = PromotionStatus.COMPLETED;
        r.finishedAt = Instant.now();
        return r;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getSourceEnvironmentId() {
        return sourceEnvironmentId;
    }

    public UUID getTargetEnvironmentId() {
        return targetEnvironmentId;
    }

    public EnvironmentType getSourceType() {
        return sourceType;
    }

    public EnvironmentType getTargetType() {
        return targetType;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public Integer getVariablesCopied() {
        return variablesCopied;
    }

    public Integer getSecretsCopied() {
        return secretsCopied;
    }

    public String getMessage() {
        return message;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
