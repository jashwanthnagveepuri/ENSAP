package com.ensap.evidenceaudit.dto;

import java.time.Instant;

/**
 * Wire shape for {@code GET /api/deployments/{deploymentId}/evidence}
 * (docs/10-api-design.md). Phase 0: shape only, not yet populated from real
 * data.
 */
public record EvidenceResponse(
        String id,
        String deploymentId,
        String stepName,
        String s3Key,
        String contentType,
        Instant createdAt
) {
}
