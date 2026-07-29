package io.nimbus.platform.deployment.domain;

public enum DeploymentTrigger {
    WIZARD_PROVISION,
    ENVIRONMENT_PROMOTE,
    MANUAL,
    PIPELINE,
    RETRY
}
