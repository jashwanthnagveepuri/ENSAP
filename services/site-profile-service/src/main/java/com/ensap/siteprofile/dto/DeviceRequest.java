package com.ensap.siteprofile.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceRequest(
        @NotBlank(message = "type is required") String type,
        String vendor,
        String model,
        String serialNumber,
        @NotBlank(message = "status is required") String status
) {
}
