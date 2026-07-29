package io.nimbus.platform.k8s.domain;

public enum DeployStatus {
    PENDING,
    DEPLOYING,
    RUNNING,
    DEGRADED,
    FAILED,
    UNKNOWN,
    SIMULATED
}
