package com.ensap.siteprofile.dto;

import java.time.Instant;

/**
 * Wire shape for {@code GET /api/sites} / {@code GET /api/sites/{siteId}}
 * (docs/10-api-design.md). Phase 0: shape only, not yet populated from real data.
 */
public record SiteResponse(
        String id,
        String name,
        String region,
        String status,
        Instant lastRefreshedAt
) {
}
