package io.nimbus.platform.catalog.service;

import io.nimbus.platform.catalog.domain.ServiceTemplate;
import io.nimbus.platform.catalog.domain.TemplateStatus;
import io.nimbus.platform.catalog.dto.CatalogDtos;
import io.nimbus.platform.catalog.repository.ServiceTemplateRepository;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CatalogService {

    private final ServiceTemplateRepository templateRepository;

    public CatalogService(ServiceTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogDtos.TemplateSummary> list(boolean publishedOnly, String q) {
        List<ServiceTemplate> templates = publishedOnly
                ? templateRepository.findByStatusAndDeletedAtIsNullOrderByOfficialDescNameAsc(TemplateStatus.PUBLISHED)
                : templateRepository.findByDeletedAtIsNullOrderByOfficialDescNameAsc();

        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase(Locale.ROOT);
            templates = templates.stream()
                    .filter(t -> t.getName().toLowerCase(Locale.ROOT).contains(needle)
                            || (t.getDescription() != null && t.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                            || (t.getTags() != null && t.getTags().toLowerCase(Locale.ROOT).contains(needle))
                            || t.getRuntime().name().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }
        return templates.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public CatalogDtos.TemplateDetail get(UUID id) {
        return toDetail(require(id));
    }

    @Transactional(readOnly = true)
    public ServiceTemplate requirePublished(UUID id) {
        ServiceTemplate template = require(id);
        if (template.getStatus() != TemplateStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_PUBLISHED);
        }
        return template;
    }

    @Transactional(readOnly = true)
    public ServiceTemplate require(UUID id) {
        return templateRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private CatalogDtos.TemplateSummary toSummary(ServiceTemplate t) {
        return new CatalogDtos.TemplateSummary(
                t.getId(), t.getName(), t.getDescription(), t.getType(), t.getRuntime(),
                t.getLanguage(), t.getLatestVersion(), t.isOfficial(), t.getStatus(), t.getTags()
        );
    }

    private CatalogDtos.TemplateDetail toDetail(ServiceTemplate t) {
        return new CatalogDtos.TemplateDetail(
                t.getId(), t.getName(), t.getDescription(), t.getType(), t.getRuntime(),
                t.getLanguage(), t.getLatestVersion(), t.isOfficial(), t.getStatus(), t.getTags(),
                t.getBlueprint(), t.getDefaultHelmValues(), t.getDefaultTerraformVars(), t.getDefaultWorkflow()
        );
    }
}
