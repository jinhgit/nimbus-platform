package io.nimbus.platform.provision.domain;

/**
 * Provision saga step codes (ordered).
 */
public enum StepCode {
    SCM_PREPARE(10, "SCM prepare"),
    SCM_REPO(20, "Repository create"),
    SCM_MANIFESTS(30, "Manifests / Actions"),
    K8S_PREPARE(40, "Kubernetes prepare"),
    K8S_DEPLOY(50, "Kubernetes deploy"),
    FINALIZE(60, "Finalize service");

    private final int order;
    private final String defaultName;

    StepCode(int order, String defaultName) {
        this.order = order;
        this.defaultName = defaultName;
    }

    public int getOrder() {
        return order;
    }

    public String getDefaultName() {
        return defaultName;
    }
}
