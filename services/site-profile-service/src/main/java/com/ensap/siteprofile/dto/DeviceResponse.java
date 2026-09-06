package com.ensap.siteprofile.dto;

import java.time.Instant;

public record DeviceResponse(
        String id,
        String siteId,
        String type,
        String vendor,
        String model,
        String serialNumber,
        String status,
        Instant createdAt
) {
}
