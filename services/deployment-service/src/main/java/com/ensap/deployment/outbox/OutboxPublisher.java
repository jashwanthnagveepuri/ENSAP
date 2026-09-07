package com.ensap.deployment.outbox;

import com.ensap.deployment.entity.OutboxEvent;
import com.ensap.deployment.repository.OutboxEventRepository;
import com.ensap.events.EventEnvelope;
import com.ensap.events.Topics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The transactional outbox's publishing half (ADR-0004, master spec §11):
 * polls {@code outbox_event} for unpublished rows and sends them to Kafka,
 * partitioned by {@code siteId} (docs/11-event-catalog.md Kafka rules).
 * At-least-once by design — a row stays unpublished (and gets retried next
 * poll) on any send failure, so a duplicate delivery on the consumer side
 * is expected and handled there via {@code processed_operation} (§12).
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${deployment.outbox.poll-interval-ms:2000}")
    public void publishPending() {
        List<OutboxEvent> pending;
        try {
            pending = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        } catch (Exception e) {
            // A transient DB blip (or the datasource going away, e.g. at shutdown) just means
            // this poll is skipped — the next one retries, same at-least-once story as publishOne.
            log.warn("Failed to poll outbox_event; will retry next poll: {}", e.toString());
            return;
        }
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEvent event) {
        String topic = topicFor(event.getAggregateType());
        if (topic == null) {
            log.warn("No Kafka topic mapped for outbox aggregate_type={}; event {} left unpublished", event.getAggregateType(), event.getId());
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            String siteId = payload.path("siteId").asText(null);
            EventEnvelope envelope = new EventEnvelope(event.getId(), event.getEventType(), event.getEventVersion(),
                    event.getCreatedAt(), event.getCorrelationId(), event.getTraceId(), siteId,
                    event.getAggregateId(), payload);
            String json = objectMapper.writeValueAsString(envelope);
            String partitionKey = siteId != null ? siteId : event.getAggregateId();

            kafkaTemplate.send(topic, partitionKey, json).get(5, TimeUnit.SECONDS);

            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            // ponytail: leave unpublished, next poll retries — no dead-letter/backoff yet, add if
            // a poisoned row is ever observed retrying forever.
            log.warn("Failed to publish outbox event {} (type={}); will retry next poll: {}",
                    event.getId(), event.getEventType(), e.toString());
        }
    }

    private static String topicFor(String aggregateType) {
        return "deployment".equals(aggregateType) ? Topics.DEPLOYMENT_EVENTS : null;
    }
}
