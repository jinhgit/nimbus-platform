package io.nimbus.platform.deployment.domain;

public enum DeploymentStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    ROLLED_BACK,
    SIMULATED
}
