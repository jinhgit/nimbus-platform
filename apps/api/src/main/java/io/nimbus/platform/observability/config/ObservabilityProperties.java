package io.nimbus.platform.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nimbus.observability")
public class ObservabilityProperties {

    private String prometheusUrl = "http://localhost:9090";
    private String grafanaUrl = "http://localhost:3001";
    private String lokiUrl = "http://localhost:3100";
    private boolean demoMetricsWhenUnavailable = true;

    public String getPrometheusUrl() {
        return prometheusUrl;
    }

    public void setPrometheusUrl(String prometheusUrl) {
        this.prometheusUrl = prometheusUrl;
    }

    public String getGrafanaUrl() {
        return grafanaUrl;
    }

    public void setGrafanaUrl(String grafanaUrl) {
        this.grafanaUrl = grafanaUrl;
    }

    public String getLokiUrl() {
        return lokiUrl;
    }

    public void setLokiUrl(String lokiUrl) {
        this.lokiUrl = lokiUrl;
    }

    public boolean isDemoMetricsWhenUnavailable() {
        return demoMetricsWhenUnavailable;
    }

    public void setDemoMetricsWhenUnavailable(boolean demoMetricsWhenUnavailable) {
        this.demoMetricsWhenUnavailable = demoMetricsWhenUnavailable;
    }
}
