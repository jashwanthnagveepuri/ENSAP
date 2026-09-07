package com.ensap.evidenceaudit;

import com.ensap.evidenceaudit.repository.AuditEventRepository;
import com.ensap.evidenceaudit.repository.ProcessedOperationRepository;
import com.ensap.events.DeploymentEventPayload;
import com.ensap.events.DeploymentEventType;
import com.ensap.events.EventEnvelope;
import com.ensap.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the consume + idempotency half of the transactional outbox loop
 * end to end (master spec §9/§12, §37 Phase 3): a real {@code deployment.*}
 * Kafka message becomes exactly one {@code audit_event} row, and a
 * redelivery of the SAME message (same eventId — the realistic at-least-once
 * scenario, e.g. a crash before offset commit) does not create a second
 * one. deployment-service's {@code OutboxPublisherIT} covers the produce
 * half — the two together are the loop this phase exists to build.
 */
class DeploymentEventConsumerIT extends AbstractIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private AuditEventRepository auditEventRepository;
    @Autowired
    private ProcessedOperationRepository processedOperationRepository;

    private Producer<String, String> producer;

    @BeforeEach
    void setUpProducer() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(KAFKA.getBootstrapServers());
        producer = new KafkaProducer<>(producerProps, new StringSerializer(), new StringSerializer());
    }

    @AfterEach
    void tearDownProducer() {
        producer.close();
    }

    @Test
    void deploymentEvent_isRecordedOnce_evenWhenRedelivered() throws Exception {
        String eventId = UUID.randomUUID().toString();
        String deploymentId = "DEP-CONSUMER-IT";
        String siteId = "SITE-CONSUMER-1";
        String json = envelopeJson(eventId, deploymentId, siteId);

        producer.send(new ProducerRecord<>(Topics.DEPLOYMENT_EVENTS, siteId, json)).get(10, TimeUnit.SECONDS);
        awaitAuditRow(deploymentId);

        // Redeliver the exact same message (same eventId) — simulates the consumer crashing
        // after processing but before committing its offset.
        producer.send(new ProducerRecord<>(Topics.DEPLOYMENT_EVENTS, siteId, json)).get(10, TimeUnit.SECONDS);
        Thread.sleep(3000); // give a broken guard a chance to double-record before asserting

        long recorded = auditEventRepository.findAll().stream().filter(a -> deploymentId.equals(a.getDeploymentId())).count();
        assertThat(recorded).as("audit_event rows for %s after redelivery", deploymentId).isEqualTo(1);
        assertThat(processedOperationRepository.existsById(eventId)).isTrue();

        var audit = auditEventRepository.findAll().stream().filter(a -> deploymentId.equals(a.getDeploymentId())).findFirst().orElseThrow();
        assertThat(audit.getSiteId()).isEqualTo(siteId);
        assertThat(audit.getEventType()).isEqualTo(DeploymentEventType.REQUESTED.type());
        assertThat(audit.getActor()).isEqualTo("consumer-it-user");
    }

    private void awaitAuditRow(String deploymentId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            boolean found = auditEventRepository.findAll().stream().anyMatch(a -> deploymentId.equals(a.getDeploymentId()));
            if (found) {
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No audit_event recorded for " + deploymentId + " within timeout");
    }

    private static String envelopeJson(String eventId, String deploymentId, String siteId) throws Exception {
        DeploymentEventPayload payload = new DeploymentEventPayload(deploymentId, siteId, "REQUESTED", "consumer-it-user", null);
        EventEnvelope envelope = new EventEnvelope(UUID.fromString(eventId), DeploymentEventType.REQUESTED.type(), 1,
                Instant.now(), UUID.randomUUID(), null, siteId, deploymentId, MAPPER.valueToTree(payload));
        return MAPPER.writeValueAsString(envelope);
    }
}
