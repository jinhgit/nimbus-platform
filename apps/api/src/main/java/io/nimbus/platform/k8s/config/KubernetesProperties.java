package io.nimbus.platform.k8s.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "nimbus.k8s")
public class KubernetesProperties {

    /**
     * false 이면 항상 시뮬레이션 (클러스터 미사용).
     */
    private boolean enabled = true;

    /**
     * 비어 있으면 ~/.kube/config 기본 경로.
     */
    private String kubeconfig = "";

    /**
     * free-only: 로컬 컨텍스트만 허용.
     */
    private boolean strictLocal = true;

    private List<String> allowedContextPatterns = new ArrayList<>(List.of(
            "k3d-.*",
            "kind-.*",
            "minikube",
            "docker-desktop",
            "orbstack"
    ));

    /**
     * 데모용 기본 이미지 (앱 빌드 없이 Pod Running 시연).
     */
    private String demoImage = "nginx:1.27-alpine";

    private int deployTimeoutSeconds = 90;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKubeconfig() {
        return kubeconfig;
    }

    public void setKubeconfig(String kubeconfig) {
        this.kubeconfig = kubeconfig;
    }

    public boolean isStrictLocal() {
        return strictLocal;
    }

    public void setStrictLocal(boolean strictLocal) {
        this.strictLocal = strictLocal;
    }

    public List<String> getAllowedContextPatterns() {
        return allowedContextPatterns;
    }

    public void setAllowedContextPatterns(List<String> allowedContextPatterns) {
        this.allowedContextPatterns = allowedContextPatterns;
    }

    public String getDemoImage() {
        return demoImage;
    }

    public void setDemoImage(String demoImage) {
        this.demoImage = demoImage;
    }

    public int getDeployTimeoutSeconds() {
        return deployTimeoutSeconds;
    }

    public void setDeployTimeoutSeconds(int deployTimeoutSeconds) {
        this.deployTimeoutSeconds = deployTimeoutSeconds;
    }
}
