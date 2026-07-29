package io.nimbus.platform.catalog.controller;

import io.nimbus.platform.catalog.dto.CatalogDtos;
import io.nimbus.platform.catalog.service.CatalogService;
import io.nimbus.platform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ApiResponse<List<CatalogDtos.TemplateSummary>> list(
            @RequestParam(defaultValue = "true") boolean publishedOnly,
            @RequestParam(required = false) String q
    ) {
        return ApiResponse.ok(catalogService.list(publishedOnly, q));
    }

    @GetMapping("/search")
    public ApiResponse<List<CatalogDtos.TemplateSummary>> search(@RequestParam String q) {
        return ApiResponse.ok(catalogService.list(true, q));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<CatalogDtos.TemplateDetail> get(@PathVariable UUID templateId) {
        return ApiResponse.ok(catalogService.get(templateId));
    }
}
