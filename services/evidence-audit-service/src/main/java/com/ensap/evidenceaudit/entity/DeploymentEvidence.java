package com.ensap.evidenceaudit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps the {@code deployment_evidence} table (docs/09-database-design.md) —
 * metadata only; the artifact body lives in S3 at {@code s3Key}
 * (docs/18-adr/0007-s3-for-evidence-and-artifacts.md). Phase 0: schema
 * shape only.
 */
@Entity
@Table(name = "deployment_evidence")
public class DeploymentEvidence {

    @Id
    private String id;

    @Column(name = "deployment_id", nullable = false)
    private String deploymentId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
