package com.ensap.evidenceaudit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Maps the {@code processed_operation} table (docs/09-database-design.md).
 * Its primary key IS the idempotency guarantee (master spec §12): the Kafka
 * consumer uses the event's {@code eventId} as {@code operationId} — a
 * redelivered event finds its row already here and is skipped instead of
 * recorded twice.
 */
@Entity
@Table(name = "processed_operation")
public class ProcessedOperation {

    @Id
    @Column(name = "operation_id")
    private String operationId;

    @Column(nullable = false)
    private String status;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public ProcessedOperation() {
    }

    public ProcessedOperation(String operationId, String status, Instant processedAt) {
        this.operationId = operationId;
        this.status = status;
        this.processedAt = processedAt;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
