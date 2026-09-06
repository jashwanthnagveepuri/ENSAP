package com.ensap.deployment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps the {@code idempotency_key} table (docs/09-database-design.md,
 * master spec §12). The primary key on {@code key} is the DB-level
 * uniqueness constraint that makes {@code POST /api/deployments} with a
 * repeated {@code Idempotency-Key} return the original deployment instead
 * of creating a duplicate.
 */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKeyRecord {

    @Id
    @Column(name = "`key`") // backtick-quoted: "key" is a reserved word in H2 (used by the Phase 0 smoke-test profile)
    private String key;

    @Column(name = "deployment_id", nullable = false)
    private String deploymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public IdempotencyKeyRecord() {
    }

    public IdempotencyKeyRecord(String key, String deploymentId, Instant createdAt) {
        this.key = key;
        this.deploymentId = deploymentId;
        this.createdAt = createdAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
