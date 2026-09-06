package com.ensap.events;

/**
 * Deployment lifecycle event types deployment-service actually produces
 * today (master spec §10 event catalog, {@code ensap.deployment.events}
 * topic). {@code deployment.validation.*} and {@code deployment.retry.*}
 * are catalog entries for Camunda/Phase 4 workers that don't exist yet —
 * add them here when those producers land, not before.
 */
public enum DeploymentEventType {
    REQUESTED("deployment.requested"),
    STARTED("deployment.started"),
    COMPLETED("deployment.completed"),
    FAILED("deployment.failed");

    private final String type;

    DeploymentEventType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
