package com.ensap.deployment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Wire shape for {@code POST /api/deployments} (docs/10-api-design.md).
 */
public record CreateDeploymentRequest(
        @NotBlank String siteId,
        String requestedBy
) {
}
