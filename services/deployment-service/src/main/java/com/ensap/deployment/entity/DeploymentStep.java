package com.ensap.deployment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Maps the {@code deployment_step} table (docs/09-database-design.md).
 * Phase 2 owns exactly one step, {@code camunda-workflow-start} — the
 * deployment-service's own attempt to hand the deployment to Camunda. The
 * per-provider steps named in the BPMN (validate-site, router-provision,
 * ...) are tracked by their Zeebe job workers starting Phase 4; this table
 * doesn't fabricate rows for those until a worker actually runs them.
 */
@Entity
@Table(name = "deployment_step", uniqueConstraints = @UniqueConstraint(columnNames = {"deployment_id", "step_name"}))
public class DeploymentStep {

    public static final String WORKFLOW_START = "camunda-workflow-start";

    @Id
    private String id;

    @Column(name = "deployment_id", nullable = false)
    private String deploymentId;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(nullable = false)
    private String status;

    @Column(name = "attempt_count")
    private int attemptCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
