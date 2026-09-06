package com.ensap.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips an envelope through Jackson so the field names actually match
 * event-envelope.schema.json (§10) instead of only compiling — this is the
 * one thing every producer/consumer trusts this module to get right.
 */
class EventEnvelopeSerdeTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void roundTripsThroughJsonWithSchemaFieldNames() throws Exception {
        DeploymentEventPayload payload = new DeploymentEventPayload("DEP-1", "SITE-1", "REQUESTED", "alice", null);
        EventEnvelope envelope = new EventEnvelope(
                UUID.randomUUID(), DeploymentEventType.REQUESTED.type(), 1, Instant.parse("2026-01-01T00:00:00Z"),
                UUID.randomUUID(), "trace-1", "SITE-1", "DEP-1", mapper.valueToTree(payload));

        String json = mapper.writeValueAsString(envelope);
        assertTrue(json.contains("\"eventType\":\"deployment.requested\""));
        assertTrue(json.contains("\"occurredAt\""));

        EventEnvelope roundTripped = mapper.readValue(json, EventEnvelope.class);
        assertEquals(envelope.eventId(), roundTripped.eventId());
        assertEquals(envelope.eventType(), roundTripped.eventType());
        assertEquals("DEP-1", roundTripped.payload().get("deploymentId").asText());
    }
}
