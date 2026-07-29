package io.nimbus.platform.incident.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "service_name", length = 120)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 24)
    private IncidentSource sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentSeverity severity = IncidentSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(length = 500)
    private String summary;

    @Lob
    @Column(name = "analysis_text")
    private String analysisText;

    @Column(name = "provider", length = 32)
    private String provider = "rule-engine";

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    protected Incident() {
    }

    public static Incident open(
            UUID workspaceId,
            UUID serviceId,
            String serviceName,
            IncidentSource sourceType,
            UUID sourceId,
            String title,
            IncidentSeverity severity,
            String summary,
            String analysisText
    ) {
        Incident i = new Incident();
        i.workspaceId = workspaceId;
        i.serviceId = serviceId;
        i.serviceName = serviceName;
        i.sourceType = sourceType;
        i.sourceId = sourceId;
        i.title = title;
        i.severity = severity != null ? severity : IncidentSeverity.MEDIUM;
        i.status = IncidentStatus.OPEN;
        i.summary = summary;
        i.analysisText = analysisText;
        i.provider = "rule-engine";
        i.openedAt = Instant.now();
        return i;
    }

    public void acknowledge(UUID userId) {
        if (this.status == IncidentStatus.RESOLVED) {
            return;
        }
        this.status = IncidentStatus.ACKNOWLEDGED;
        this.acknowledgedBy = userId;
    }

    public void resolve() {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public IncidentSource getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getTitle() {
        return title;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getAnalysisText() {
        return analysisText;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public UUID getAcknowledgedBy() {
        return acknowledgedBy;
    }
}
