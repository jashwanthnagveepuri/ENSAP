package com.ensap.deployment.workflow;

/**
 * Outcome of asking Camunda to start (or the app to deploy)
 * {@code deployment-process} (workflow/camunda/deployment-process.bpmn).
 * {@code DISABLED} means Camunda integration is turned off
 * ({@code deployment.camunda.enabled=false}, used by tests that don't run a
 * Zeebe broker) — distinct from {@code ERROR}, a real attempt that failed
 * (e.g. broker unreachable), which the deployment state machine treats as a
 * retryable failure.
 */
public record StartResult(Outcome outcome, String workflowInstanceId, String errorMessage) {

    public enum Outcome { STARTED, DISABLED, ERROR }

    public static StartResult started(String workflowInstanceId) {
        return new StartResult(Outcome.STARTED, workflowInstanceId, null);
    }

    public static StartResult disabled() {
        return new StartResult(Outcome.DISABLED, null, null);
    }

    public static StartResult error(String message) {
        return new StartResult(Outcome.ERROR, null, message);
    }
}
