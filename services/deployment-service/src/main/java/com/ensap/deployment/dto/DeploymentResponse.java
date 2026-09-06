package com.ensap.deployment.dto;

import java.time.Instant;
import java.util.List;

/**
 * Wire shape for deployment endpoints (docs/10-api-design.md).
 */
public record DeploymentResponse(
        String id,
        String siteId,
        String batchId,
        String status,
        String workflowInstanceId,
        String requestedBy,
        Instant createdAt,
        Instant updatedAt,
        List<DeploymentStepResponse> steps
) {
}
