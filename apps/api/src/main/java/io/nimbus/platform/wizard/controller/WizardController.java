package io.nimbus.platform.wizard.controller;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.provision.dto.ProvisionDtos;
import io.nimbus.platform.wizard.dto.WizardDtos;
import io.nimbus.platform.wizard.service.WizardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service-wizard")
public class WizardController {

    private final WizardService wizardService;

    public WizardController(WizardService wizardService) {
        this.wizardService = wizardService;
    }

    @PostMapping
    public ApiResponse<WizardDtos.WizardResponse> create(
            @Valid @RequestBody WizardDtos.CreateWizardRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.create(principal, request));
    }

    @GetMapping("/{wizardId}")
    public ApiResponse<WizardDtos.WizardResponse> get(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.get(principal, wizardId));
    }

    @GetMapping
    public ApiResponse<List<WizardDtos.WizardResponse>> history(
            @RequestParam(required = false) UUID projectId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.history(principal, projectId));
    }

    @PatchMapping("/{wizardId}")
    public ApiResponse<WizardDtos.WizardResponse> update(
            @PathVariable UUID wizardId,
            @Valid @RequestBody WizardDtos.UpdateWizardRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.update(principal, wizardId, request));
    }

    @PostMapping("/{wizardId}/recommend")
    public ApiResponse<AiDtos.RecommendationResponse> recommend(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.recommend(principal, wizardId));
    }

    @PostMapping("/{wizardId}/validate")
    public ApiResponse<WizardDtos.ValidateResponse> validate(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.validate(principal, wizardId));
    }

    @PostMapping("/{wizardId}/preview")
    public ApiResponse<WizardDtos.PreviewResponse> preview(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.preview(principal, wizardId));
    }

    @PostMapping("/{wizardId}/execute")
    public ApiResponse<WizardDtos.ExecuteResponse> execute(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.execute(principal, wizardId));
    }

    @PostMapping("/{wizardId}/cancel")
    public ApiResponse<WizardDtos.WizardResponse> cancel(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.cancel(principal, wizardId));
    }

    @PostMapping("/{wizardId}/retry")
    public ApiResponse<WizardDtos.ExecuteResponse> retry(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.retry(principal, wizardId));
    }

    @GetMapping("/{wizardId}/saga")
    public ApiResponse<ProvisionDtos.SagaResponse> latestSaga(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.latestSaga(principal, wizardId));
    }

    @GetMapping("/{wizardId}/sagas")
    public ApiResponse<List<ProvisionDtos.SagaResponse>> listSagas(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.listSagas(principal, wizardId));
    }

    @GetMapping("/{wizardId}/logs")
    public ApiResponse<WizardDtos.WizardLogsResponse> logs(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.logs(principal, wizardId));
    }

    @GetMapping("/{wizardId}/status")
    public ApiResponse<WizardDtos.WizardResponse> status(@PathVariable UUID wizardId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(wizardService.get(principal, wizardId));
    }
}
