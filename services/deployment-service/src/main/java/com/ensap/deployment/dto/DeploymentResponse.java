package com.ensap.deployment.dto;

import java.time.Instant;

/**
 * Wire shape for deployment endpoints (docs/10-api-design.md). Phase 0:
 * shape only, not yet populated from real data.
 */
public record DeploymentResponse(
        String id,
        String siteId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
