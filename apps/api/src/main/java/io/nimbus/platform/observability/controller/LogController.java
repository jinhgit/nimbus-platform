package io.nimbus.platform.observability.controller;

import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.common.api.ApiResponse;
import io.nimbus.platform.observability.dto.ObservabilityDtos;
import io.nimbus.platform.observability.service.LogStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    private final LogStreamService logStreamService;

    public LogController(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    @GetMapping("/services/{serviceId}")
    public ApiResponse<ObservabilityDtos.LogSnapshot> snapshot(
            @PathVariable UUID serviceId,
            @RequestParam(defaultValue = "100") int limit
    ) {
        SecurityUtils.requirePrincipal();
        return ApiResponse.ok(logStreamService.snapshot(serviceId, limit));
    }

    @GetMapping(path = "/services/{serviceId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID serviceId) {
        SecurityUtils.requirePrincipal();
        return logStreamService.stream(serviceId);
    }
}
