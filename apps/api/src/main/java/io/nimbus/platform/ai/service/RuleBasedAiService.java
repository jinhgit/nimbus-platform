package io.nimbus.platform.ai.service;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * free-only AI Decision Engine 기본 구현.
 * Ollama 없이도 시연 가능한 Rule + Confidence 엔진.
 */
@Service
public class RuleBasedAiService {

    public AiDtos.RecommendationResponse recommend(AiDtos.RecommendationRequest request) {
        RuntimeType runtime = request.runtime() != null ? request.runtime() : RuntimeType.SPRING_BOOT;
        EnvironmentType env = request.environmentType() != null
                ? request.environmentType()
                : EnvironmentType.DEV;
        String traffic = request.expectedTraffic() != null
                ? request.expectedTraffic().toUpperCase(Locale.ROOT)
                : "MEDIUM";
        String name = request.serviceName() != null ? request.serviceName().toLowerCase(Locale.ROOT) : "";
        String desc = request.description() != null ? request.description().toLowerCase(Locale.ROOT) : "";

        boolean paymentLike = name.contains("payment") || name.contains("order")
                || desc.contains("결제") || desc.contains("payment") || desc.contains("쇼핑몰");
        boolean highTraffic = "HIGH".equals(traffic) || env == EnvironmentType.PRODUCTION || paymentLike;

        String database = "POSTGRES";
        int dbConfidence = paymentLike || runtime == RuntimeType.SPRING_BOOT ? 96 : 88;
        String cache = highTraffic || paymentLike ? "REDIS" : "NONE";
        int cacheConfidence = cache.equals("REDIS") ? 93 : 70;
        int replicas = env == EnvironmentType.PRODUCTION ? (highTraffic ? 3 : 2) : 1;
        boolean hpa = env == EnvironmentType.PRODUCTION;
        String cpu = env == EnvironmentType.PRODUCTION ? "500m" : "250m";
        String memory = runtime == RuntimeType.SPRING_BOOT
                ? (env == EnvironmentType.PRODUCTION ? "1Gi" : "512Mi")
                : "256Mi";

        int runtimeConfidence = switch (runtime) {
            case SPRING_BOOT -> 97;
            case NESTJS, NEXTJS -> 92;
            case FASTAPI -> 90;
            default -> 85;
        };

        List<AiDtos.RecommendationItem> items = new ArrayList<>();
        items.add(new AiDtos.RecommendationItem(
                "runtime", runtime.name(), runtimeConfidence,
                runtime == RuntimeType.SPRING_BOOT
                        ? "Java 기반 REST API에 적합합니다."
                        : runtime.name() + " 템플릿 Golden Path 권장"
        ));
        items.add(new AiDtos.RecommendationItem(
                "database", database, dbConfidence,
                "트랜잭션·관계형 데이터에 PostgreSQL을 권장합니다."
        ));
        items.add(new AiDtos.RecommendationItem(
                "cache", cache, cacheConfidence,
                cache.equals("REDIS")
                        ? "조회 부하·세션 캐시를 위해 Redis를 권장합니다."
                        : "초기 트래픽이 낮아 Cache는 선택 사항입니다."
        ));
        items.add(new AiDtos.RecommendationItem(
                "replica", String.valueOf(replicas), env == EnvironmentType.PRODUCTION ? 95 : 80,
                env == EnvironmentType.PRODUCTION
                        ? "Production 환경에서는 최소 " + Math.max(replicas, 2) + " Replica 권장 (HA)."
                        : "Dev 환경은 비용 효율을 위해 1 Replica."
        ));
        items.add(new AiDtos.RecommendationItem(
                "hpa", String.valueOf(hpa), 91,
                hpa ? "트래픽 증가 가능성에 대비해 HPA를 활성화합니다." : "Dev에서는 HPA 비활성 기본."
        ));

        String summary = highTraffic
                ? "트래픽이 증가할 가능성이 높습니다. HA·캐시·HPA를 함께 구성하세요."
                : "기본 Golden Path 구성을 권장합니다.";

        int overall = (runtimeConfidence + dbConfidence + cacheConfidence) / 3;

        return new AiDtos.RecommendationResponse(
                runtime,
                runtimeConfidence,
                database,
                dbConfidence,
                cache,
                cacheConfidence,
                replicas,
                hpa,
                cpu,
                memory,
                overall,
                summary,
                items,
                "rule-engine"
        );
    }

    public AiDtos.ArchitectureReviewResponse review(ServiceWizard wizard) {
        List<String> strengths = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        int score = 88;

        strengths.add("Service Wizard + Catalog Golden Path 사용");
        if (wizard.getRuntime() != null) {
            strengths.add("Runtime 명시: " + wizard.getRuntime());
        }

        if (wizard.getEnvironmentType() == EnvironmentType.PRODUCTION
                && (wizard.getReplicaCount() == null || wizard.getReplicaCount() < 2)) {
            risks.add("Production + Replica 1 → HA 불가");
            recommendations.add("Replica 2 이상으로 상향");
            score -= 8;
        }
        if (wizard.getHpaEnabled() == null || !wizard.getHpaEnabled()) {
            if (wizard.getEnvironmentType() == EnvironmentType.PRODUCTION) {
                recommendations.add("HPA 활성화");
                score -= 4;
            }
        } else {
            strengths.add("HPA 활성화");
            score += 2;
        }
        if (wizard.getCacheType() == null || "NONE".equalsIgnoreCase(wizard.getCacheType())) {
            recommendations.add("Redis Cache 추가 검토");
            score -= 3;
        } else {
            strengths.add("Cache 구성: " + wizard.getCacheType());
            score += 2;
        }
        if (wizard.getDatabaseType() != null) {
            strengths.add("Database: " + wizard.getDatabaseType());
        }
        recommendations.add("latest 태그 금지 — 고정 버전 태그 사용");
        recommendations.add("Readiness / Liveness Probe 유지");

        score = Math.max(40, Math.min(99, score));

        return new AiDtos.ArchitectureReviewResponse(
                score,
                strengths,
                risks,
                recommendations,
                "rule-engine"
        );
    }
}
