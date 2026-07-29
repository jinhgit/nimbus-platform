package io.nimbus.platform.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nimbus.ai")
public class AiProperties {

    /**
     * rule | ollama
     */
    private String provider = "rule";

    private String ollamaBaseUrl = "http://localhost:11434";

    private String ollamaModel = "llama3.2";

    private int ollamaTimeoutMs = 8000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    public int getOllamaTimeoutMs() {
        return ollamaTimeoutMs;
    }

    public void setOllamaTimeoutMs(int ollamaTimeoutMs) {
        this.ollamaTimeoutMs = ollamaTimeoutMs;
    }

    public boolean useOllama() {
        return provider != null && provider.equalsIgnoreCase("ollama");
    }
}
