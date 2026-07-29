package io.nimbus.platform.pipeline.controller;

import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.pipeline.dto.PipelineDtos;
import io.nimbus.platform.pipeline.service.BuildPipelineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipelines")
public class PipelineController {

    private final BuildPipelineService pipelineService;

    public PipelineController(BuildPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping
    public ApiResponse<PipelineDtos.PipelineResponse> create(
            @Valid @RequestBody PipelineDtos.CreatePipelineRequest request
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(pipelineService.createAndRun(principal, request.serviceId()));
    }

    @GetMapping
    public ApiResponse<List<PipelineDtos.PipelineResponse>> list(
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) UUID serviceId
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(pipelineService.list(principal, workspaceId, serviceId));
    }

    @GetMapping("/{pipelineId}")
    public ApiResponse<PipelineDtos.PipelineResponse> get(@PathVariable UUID pipelineId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(pipelineService.get(principal, pipelineId));
    }

    @GetMapping("/{pipelineId}/logs")
    public ApiResponse<PipelineDtos.PipelineLogsResponse> logs(@PathVariable UUID pipelineId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(pipelineService.logs(principal, pipelineId));
    }

    @PostMapping("/{pipelineId}/rerun")
    public ApiResponse<PipelineDtos.PipelineResponse> rerun(@PathVariable UUID pipelineId) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(pipelineService.rerun(principal, pipelineId));
    }
}
