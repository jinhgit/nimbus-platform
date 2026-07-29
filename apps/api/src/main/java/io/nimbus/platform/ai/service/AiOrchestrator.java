package io.nimbus.platform.ai.service;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Routes AI calls: Ollama when configured+reachable, else rule-engine.
 * free-only: never fail hard on Ollama down.
 */
@Service
public class AiOrchestrator {

    private final RuleBasedAiService ruleBasedAiService;
    private final YamlExplainService yamlExplainService;
    private final OllamaClient ollamaClient;

    public AiOrchestrator(
            RuleBasedAiService ruleBasedAiService,
            YamlExplainService yamlExplainService,
            OllamaClient ollamaClient
    ) {
        this.ruleBasedAiService = ruleBasedAiService;
        this.yamlExplainService = yamlExplainService;
        this.ollamaClient = ollamaClient;
    }

    public AiDtos.RecommendationResponse recommend(AiDtos.RecommendationRequest request) {
        AiDtos.RecommendationResponse base = ruleBasedAiService.recommend(request);
        Optional<String> llm = ollamaClient.generate(
                "You are an IDP assistant. In 2 short Korean sentences, refine this infra recommendation for service "
                        + request.serviceName() + ": runtime=" + base.runtime()
                        + ", db=" + base.database() + ", cache=" + base.cache()
                        + ", replicas=" + base.replicaCount() + ". Base summary: " + base.summary()
        );
        if (llm.isEmpty()) {
            return base;
        }
        return new AiDtos.RecommendationResponse(
                base.runtime(),
                base.runtimeConfidence(),
                base.database(),
                base.databaseConfidence(),
                base.cache(),
                base.cacheConfidence(),
                base.replicaCount(),
                base.hpaEnabled(),
                base.cpu(),
                base.memory(),
                base.overallScore(),
                llm.get(),
                base.items(),
                "ollama+" + base.provider()
        );
    }

    public AiDtos.ArchitectureReviewResponse review(ServiceWizard wizard) {
        AiDtos.ArchitectureReviewResponse base = ruleBasedAiService.review(wizard);
        Optional<String> llm = ollamaClient.generate(
                "In 3 short Korean bullet points, comment on this service architecture: "
                        + "name=" + wizard.getServiceName()
                        + ", runtime=" + wizard.getRuntime()
                        + ", env=" + wizard.getEnvironmentType()
                        + ", risks=" + base.risks()
        );
        if (llm.isEmpty()) {
            return base;
        }
        List<String> recommendations = new ArrayList<>(base.recommendations());
        recommendations.add(0, "Ollama: " + truncate(llm.get(), 280));
        return new AiDtos.ArchitectureReviewResponse(
                base.score(),
                base.strengths(),
                base.risks(),
                recommendations,
                "ollama+" + base.provider()
        );
    }

    public AiDtos.YamlExplainResponse explain(AiDtos.YamlExplainRequest request) {
        AiDtos.YamlExplainResponse base = yamlExplainService.explain(request);
        Optional<String> llm = ollamaClient.generate(
                "Summarize this Kubernetes/GitOps YAML for a developer in 2 Korean sentences. "
                        + "Kind=" + base.detectedKind() + ". Snippet:\n"
                        + request.content().substring(0, Math.min(1200, request.content().length()))
        );
        if (llm.isEmpty()) {
            return base;
        }
        return new AiDtos.YamlExplainResponse(
                llm.get(),
                base.detectedKind(),
                base.highlights(),
                base.risks(),
                base.suggestions(),
                "ollama+" + base.provider()
        );
    }

    public AiDtos.AiStatusResponse status() {
        boolean ollamaUp = ollamaClient.configuredProvider().equalsIgnoreCase("ollama")
                && ollamaClient.isReachable();
        return new AiDtos.AiStatusResponse(
                ollamaClient.configuredProvider(),
                ollamaUp ? "ollama" : "rule-engine",
                ollamaClient.baseUrl(),
                ollamaClient.model(),
                ollamaUp,
                ollamaUp
                        ? "Ollama reachable — LLM 보조 설명 활성"
                        : "Rule engine 기본 (Ollama 미설정 또는 오프라인)"
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        String one = s.replace('\n', ' ').trim();
        return one.length() <= max ? one : one.substring(0, max) + "…";
    }
}
