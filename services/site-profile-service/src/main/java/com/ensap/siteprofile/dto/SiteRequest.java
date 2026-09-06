package com.ensap.siteprofile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Body for {@code POST /api/sites} and {@code PUT /api/sites/{siteId}}.
 * {@code id} is optional on create (server generates a {@code SITE-*} id
 * via {@link com.ensap.siteprofile.util.IdGenerator} when omitted).
 */
public record SiteRequest(
        String id,
        @NotBlank(message = "name is required") String name,
        String region,
        @NotBlank(message = "status is required")
        @Pattern(regexp = "ACTIVE|PENDING|DECOMMISSIONED", message = "status must be ACTIVE, PENDING, or DECOMMISSIONED")
        String status,
        List<WanCircuitRequest> wanCircuits
) {
}
