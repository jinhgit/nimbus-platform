package io.nimbus.platform.wizard.domain;

public enum WizardStatus {
    DRAFT,
    VALIDATING,
    PROVISIONING,
    DEPLOYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
