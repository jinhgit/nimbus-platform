package io.nimbus.platform.ai.controller;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.ai.service.AiOrchestrator;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiOrchestrator aiOrchestrator;
    private final ServiceWizardRepository wizardRepository;

    public AiController(
            AiOrchestrator aiOrchestrator,
            ServiceWizardRepository wizardRepository
    ) {
        this.aiOrchestrator = aiOrchestrator;
        this.wizardRepository = wizardRepository;
    }

    @GetMapping("/status")
    public ApiResponse<AiDtos.AiStatusResponse> status() {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(aiOrchestrator.status());
    }

    @PostMapping("/recommend")
    public ApiResponse<AiDtos.RecommendationResponse> recommend(
            @Valid @RequestBody AiDtos.RecommendationRequest request
    ) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(aiOrchestrator.recommend(request));
    }

    @PostMapping("/architecture-review/{wizardId}")
    public ApiResponse<AiDtos.ArchitectureReviewResponse> review(@PathVariable UUID wizardId) {
        SecurityUtils.requirePrincipal();
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WIZARD_NOT_FOUND));
        return ApiResponse.ok(aiOrchestrator.review(wizard));
    }

    /**
     * YAML Explain — rule-engine 기본, Ollama 활성 시 요약 보조.
     */
    @PostMapping("/yaml/explain")
    public ApiResponse<AiDtos.YamlExplainResponse> explainYaml(
            @Valid @RequestBody AiDtos.YamlExplainRequest request
    ) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(aiOrchestrator.explain(request));
    }
}
