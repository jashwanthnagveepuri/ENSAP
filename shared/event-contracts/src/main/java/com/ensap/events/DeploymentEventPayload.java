package com.ensap.events;

/**
 * Payload shape for every {@code deployment.*} event (master spec §10).
 * Ponytail: one record shared by requested/started/completed/failed since
 * their fields are identical today — split into per-type payloads only once
 * a type needs materially different fields. {@link EventEnvelope#eventType()}
 * says which transition this is, not this record.
 */
public record DeploymentEventPayload(
        String deploymentId,
        String siteId,
        String status,
        String requestedBy,
        String errorMessage
) {
}
