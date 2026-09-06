package com.ensap.evidenceaudit.dto;

import java.time.Instant;

/**
 * Wire shape for {@code GET /api/audit-events} (docs/10-api-design.md).
 * Phase 0: shape only, not yet populated from real data.
 */
public record AuditEventResponse(
        String id,
        String deploymentId,
        String siteId,
        String actor,
        String eventType,
        Instant createdAt
) {
}
