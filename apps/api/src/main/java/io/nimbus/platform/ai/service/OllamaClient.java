package io.nimbus.platform.ai.service;

import io.nimbus.platform.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Ollama HTTP client. Never throws for ops failures — returns empty.
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OllamaClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getOllamaBaseUrl())
                .build();
    }

    public boolean isReachable() {
        try {
            restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public Optional<String> generate(String prompt) {
        if (!properties.useOllama()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.getOllamaModel());
            body.put("prompt", prompt);
            body.put("stream", false);
            String raw = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(raw);
            String response = node.path("response").asText(null);
            if (response == null || response.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(response.trim());
        } catch (Exception ex) {
            log.info("Ollama generate failed, falling back to rules: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String statusLabel() {
        if (!properties.useOllama()) {
            return "rule";
        }
        return isReachable() ? "ollama" : "ollama-unavailable";
    }

    public String configuredProvider() {
        return properties.getProvider() != null ? properties.getProvider() : "rule";
    }

    public String model() {
        return properties.getOllamaModel();
    }

    public String baseUrl() {
        return properties.getOllamaBaseUrl();
    }
}
