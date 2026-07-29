package io.nimbus.platform.catalog.domain;

import io.nimbus.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_templates")
public class ServiceTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TemplateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RuntimeType runtime;

    @Column(nullable = false, length = 32)
    private String language;

    @Column(name = "latest_version", nullable = false, length = 20)
    private String latestVersion = "1.0.0";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TemplateStatus status = TemplateStatus.DRAFT;

    @Column(nullable = false)
    private boolean official = false;

    @Lob
    @Column(nullable = false)
    private String blueprint;

    @Lob
    private String defaultHelmValues;

    @Lob
    private String defaultTerraformVars;

    @Lob
    private String defaultWorkflow;

    @Column(length = 200)
    private String tags;

    protected ServiceTemplate() {
    }

    public static ServiceTemplate create(
            String name,
            String description,
            TemplateType type,
            RuntimeType runtime,
            String language,
            boolean official,
            String blueprint,
            String defaultHelmValues,
            String defaultTerraformVars,
            String defaultWorkflow,
            String tags
    ) {
        ServiceTemplate t = new ServiceTemplate();
        t.name = name;
        t.description = description;
        t.type = type;
        t.runtime = runtime;
        t.language = language;
        t.official = official;
        t.blueprint = blueprint;
        t.defaultHelmValues = defaultHelmValues;
        t.defaultTerraformVars = defaultTerraformVars;
        t.defaultWorkflow = defaultWorkflow;
        t.tags = tags;
        t.status = TemplateStatus.PUBLISHED;
        t.latestVersion = "1.0.0";
        return t;
    }

    public void publish() {
        this.status = TemplateStatus.PUBLISHED;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public TemplateType getType() {
        return type;
    }

    public RuntimeType getRuntime() {
        return runtime;
    }

    public String getLanguage() {
        return language;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public boolean isOfficial() {
        return official;
    }

    public String getBlueprint() {
        return blueprint;
    }

    public String getDefaultHelmValues() {
        return defaultHelmValues;
    }

    public String getDefaultTerraformVars() {
        return defaultTerraformVars;
    }

    public String getDefaultWorkflow() {
        return defaultWorkflow;
    }

    public String getTags() {
        return tags;
    }
}
