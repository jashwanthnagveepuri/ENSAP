package com.ensap.evidenceaudit.kafka;

import com.ensap.evidenceaudit.entity.AuditEvent;
import com.ensap.evidenceaudit.entity.ProcessedOperation;
import com.ensap.evidenceaudit.repository.AuditEventRepository;
import com.ensap.evidenceaudit.repository.ProcessedOperationRepository;
import com.ensap.evidenceaudit.util.IdGenerator;
import com.ensap.events.EventEnvelope;
import com.ensap.events.Topics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Consumes {@code deployment.*} domain events (master spec §9/§37 Phase 3)
 * and records them as {@code audit_event} rows. Idempotent by design
 * (§12): {@link EventEnvelope#eventId()} becomes the
 * {@code processed_operation} primary key, and the duplicate-check + audit
 * insert happen in one transaction, so a redelivered event is a no-op
 * instead of a second audit row — Kafka consumer-group ordering means the
 * same partition is never processed by two threads at once, so a plain
 * exists-then-insert (no extra locking) is sufficient.
 */
@Component
public class DeploymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeploymentEventConsumer.class);

    private final ProcessedOperationRepository processedOperationRepository;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public DeploymentEventConsumer(ProcessedOperationRepository processedOperationRepository,
                                    AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.processedOperationRepository = processedOperationRepository;
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.DEPLOYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id:evidence-audit-service}")
    public void onMessage(String message) {
        EventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message, EventEnvelope.class);
        } catch (JsonProcessingException e) {
            // A malformed message can't be fixed by retrying — log and move on rather than
            // block the partition forever. No poison-message quarantine/DLT yet (§37 Phase 8).
            log.error("Dropping unparseable deployment event: {}", e.toString());
            return;
        }
        process(envelope);
    }

    @Transactional
    public void process(EventEnvelope envelope) {
        String operationId = envelope.eventId().toString();
        if (processedOperationRepository.existsById(operationId)) {
            log.info("Duplicate delivery of event {} ({}); skipping", operationId, envelope.eventType());
            return;
        }
        processedOperationRepository.save(new ProcessedOperation(operationId, "PROCESSED", Instant.now()));

        AuditEvent audit = new AuditEvent();
        audit.setId(IdGenerator.next("AUD"));
        audit.setDeploymentId(envelope.deploymentId());
        audit.setSiteId(envelope.siteId());
        audit.setActor(actorOf(envelope));
        audit.setEventType(envelope.eventType());
        audit.setDetails(envelope.payload() != null ? envelope.payload().toString() : null);
        audit.setCreatedAt(envelope.occurredAt() != null ? envelope.occurredAt() : Instant.now());
        auditEventRepository.save(audit);
    }

    /** Prefers the deployment's requestedBy (present on every deployment.* payload) over a generic system actor. */
    private static String actorOf(EventEnvelope envelope) {
        if (envelope.payload() != null) {
            String requestedBy = envelope.payload().path("requestedBy").asText(null);
            if (requestedBy != null && !requestedBy.isBlank()) {
                return requestedBy;
            }
        }
        return "deployment-service";
    }
}
