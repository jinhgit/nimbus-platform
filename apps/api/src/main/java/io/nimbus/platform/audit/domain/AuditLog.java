package io.nimbus.platform.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 불변 감사 로그. Soft delete 없음 (PRD: Audit 보존).
 */
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_workspace_created", columnList = "workspace_id, created_at"),
                @Index(name = "idx_audit_actor_created", columnList = "actor_id, created_at"),
                @Index(name = "idx_audit_action_created", columnList = "action, created_at"),
                @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id")
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_name", length = 120)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48, columnDefinition = "varchar(48)")
    private AuditAction action;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditResult result = AuditResult.SUCCESS;

    @Column(length = 1000)
    private String message;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLog() {
    }

    public static AuditLog of(
            UUID actorId,
            String actorEmail,
            String actorName,
            AuditAction action,
            String resourceType,
            UUID resourceId,
            String resourceName,
            UUID workspaceId,
            AuditResult result,
            String message,
            String ipAddress,
            String userAgent
    ) {
        AuditLog log = new AuditLog();
        log.actorId = actorId;
        log.actorEmail = actorEmail;
        log.actorName = actorName;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.resourceName = resourceName;
        log.workspaceId = workspaceId;
        log.result = result != null ? result : AuditResult.SUCCESS;
        log.message = message;
        log.ipAddress = ipAddress;
        log.userAgent = userAgent != null && userAgent.length() > 512
                ? userAgent.substring(0, 512)
                : userAgent;
        return log;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getActorName() {
        return actorName;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public AuditResult getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
