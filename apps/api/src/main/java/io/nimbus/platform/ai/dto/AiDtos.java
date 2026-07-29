package io.nimbus.platform.ai.dto;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;

import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record RecommendationRequest(
            String serviceName,
            String description,
            RuntimeType runtime,
            EnvironmentType environmentType,
            String expectedTraffic
    ) {
    }

    public record RecommendationItem(
            String key,
            String value,
            int confidence,
            String reason
    ) {
    }

    public record RecommendationResponse(
            RuntimeType runtime,
            int runtimeConfidence,
            String database,
            int databaseConfidence,
            String cache,
            int cacheConfidence,
            int replicaCount,
            boolean hpaEnabled,
            String cpu,
            String memory,
            int overallScore,
            String summary,
            List<RecommendationItem> items,
            String provider
    ) {
    }

    public record ArchitectureReviewResponse(
            int score,
            List<String> strengths,
            List<String> risks,
            List<String> recommendations,
            String provider
    ) {
    }
}
