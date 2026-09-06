package com.ensap.deployment.entity;

/**
 * Deployment lifecycle states (master spec §37 Phase 2, docs/08-workflow.md).
 * Stored on {@link Deployment#getStatus()} as its {@code name()}.
 */
public enum DeploymentStatus {
    REQUESTED,
    RUNNING,
    COMPLETED,
    FAILED,
    FAILED_REQUIRES_ATTENTION,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean isRetryable() {
        return this == FAILED || this == FAILED_REQUIRES_ATTENTION;
    }
}
