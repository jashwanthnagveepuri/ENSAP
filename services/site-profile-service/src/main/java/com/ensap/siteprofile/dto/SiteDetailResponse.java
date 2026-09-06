package com.ensap.siteprofile.dto;

import java.time.Instant;
import java.util.List;

/**
 * Full "site profile" shape for {@code GET /api/sites/{siteId}} and the
 * refresh endpoint — {@link SiteResponse} plus owned devices/network
 * profiles/WAN circuits (master spec §7.1).
 */
public record SiteDetailResponse(
        String id,
        String name,
        String region,
        String status,
        String sourceSystem,
        Instant lastRefreshedAt,
        List<DeviceResponse> devices,
        List<NetworkProfileResponse> networkProfiles,
        List<WanCircuitResponse> wanCircuits
) {
}
