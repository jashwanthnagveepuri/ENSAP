package com.ensap.deployment;

import com.ensap.deployment.dto.CreateDeploymentRequest;
import com.ensap.events.EventEnvelope;
import com.ensap.events.Topics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the produce half of the transactional outbox loop end to end
 * (ADR-0004, master spec §11/§37 Phase 3): create a deployment over real
 * HTTP -> outbox_event row (Postgres, same transaction) -> OutboxPublisher
 * -> a real Kafka topic. evidence-audit-service's
 * {@code DeploymentEventConsumerIT} covers the consume + idempotency half —
 * the two together can't share one Spring context (they're separate Boot
 * applications), so each module proves its own half of the loop.
 */
class OutboxPublisherIT extends AbstractIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("deployment.outbox.poll-interval-ms", () -> "300");
    }

    @Autowired
    private WebTestClient client;

    @Test
    void createDeployment_publishesDeploymentRequestedToKafka() throws Exception {
        String idempotencyKey = "outbox-it-" + System.nanoTime();
        String[] deploymentId = new String[1];

        client.post().uri("/api/deployments")
                .header("Idempotency-Key", idempotencyKey)
                .bodyValue(new CreateDeploymentRequest("SITE-OUTBOX-1", "outbox-it"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> deploymentId[0] = (String) id);

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(), "outbox-it-consumer-" + System.nanoTime(), "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(Collections.singletonList(Topics.DEPLOYMENT_EVENTS));

            EventEnvelope found = null;
            long deadline = System.currentTimeMillis() + 20_000;
            while (found == null && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    EventEnvelope envelope = mapper.readValue(record.value(), EventEnvelope.class);
                    if (deploymentId[0].equals(envelope.deploymentId())) {
                        found = envelope;
                        break;
                    }
                }
            }

            assertThat(found).as("deployment.requested event for %s on %s", deploymentId[0], Topics.DEPLOYMENT_EVENTS).isNotNull();
            assertThat(found.eventType()).isEqualTo("deployment.requested");
            assertThat(found.siteId()).isEqualTo("SITE-OUTBOX-1");
        }
    }
}
