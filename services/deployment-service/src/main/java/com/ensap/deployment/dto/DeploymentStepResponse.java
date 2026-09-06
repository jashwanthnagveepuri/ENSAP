package com.ensap.deployment.dto;

import java.time.Instant;

/**
 * Wire shape for a deployment's step sub-resource (docs/10-api-design.md).
 */
public record DeploymentStepResponse(
        String id,
        String stepName,
        String status,
        int attemptCount,
        String lastError,
        Instant startedAt,
        Instant completedAt
) {
}
