package com.ensap.siteprofile.dto;

import java.time.Instant;
import java.util.List;

public record NetworkProfileResponse(
        String id,
        String siteId,
        String profileName,
        Instant createdAt,
        List<VlanResponse> vlans,
        List<SubnetResponse> subnets
) {
}
