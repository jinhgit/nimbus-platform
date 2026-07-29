package io.nimbus.platform.ai.controller;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.ai.service.RuleBasedAiService;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final RuleBasedAiService aiService;
    private final ServiceWizardRepository wizardRepository;

    public AiController(RuleBasedAiService aiService, ServiceWizardRepository wizardRepository) {
        this.aiService = aiService;
        this.wizardRepository = wizardRepository;
    }

    @PostMapping("/recommend")
    public ApiResponse<AiDtos.RecommendationResponse> recommend(
            @Valid @RequestBody AiDtos.RecommendationRequest request
    ) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(aiService.recommend(request));
    }

    @PostMapping("/architecture-review/{wizardId}")
    public ApiResponse<AiDtos.ArchitectureReviewResponse> review(@PathVariable UUID wizardId) {
        SecurityUtils.requirePrincipal();
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WIZARD_NOT_FOUND));
        return ApiResponse.ok(aiService.review(wizard));
    }
}
