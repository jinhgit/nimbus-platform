package io.nimbus.platform.catalog.dto;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.catalog.domain.TemplateStatus;
import io.nimbus.platform.catalog.domain.TemplateType;

import java.util.UUID;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record TemplateSummary(
            UUID id,
            String name,
            String description,
            TemplateType type,
            RuntimeType runtime,
            String language,
            String latestVersion,
            boolean official,
            TemplateStatus status,
            String tags
    ) {
    }

    public record TemplateDetail(
            UUID id,
            String name,
            String description,
            TemplateType type,
            RuntimeType runtime,
            String language,
            String latestVersion,
            boolean official,
            TemplateStatus status,
            String tags,
            String blueprint,
            String defaultHelmValues,
            String defaultTerraformVars,
            String defaultWorkflow
    ) {
    }
}
