package io.nimbus.platform.provision.domain;

public enum SagaStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    ROLLING_BACK,
    ROLLED_BACK,
    CANCELLED
}
