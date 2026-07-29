package io.nimbus.platform.provision.domain;

public enum StepStatus {
    WAITING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED,
    ROLLBACK,
    ROLLED_BACK
}
