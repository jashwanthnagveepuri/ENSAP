package com.ensap.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Java form of {@code shared/event-contracts/schemas/event-envelope.schema.json}
 * (master spec §10) — the common envelope for every ENSAP domain event.
 * Jackson maps record components to JSON properties of the same name, so
 * this serializes to exactly the schema's field set with no extra mapping
 * code required.
 */
public record EventEnvelope(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        UUID correlationId,
        String traceId,
        String siteId,
        String deploymentId,
        JsonNode payload
) {
}
