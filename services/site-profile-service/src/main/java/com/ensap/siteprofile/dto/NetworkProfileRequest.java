package com.ensap.siteprofile.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record NetworkProfileRequest(
        @NotBlank(message = "profileName is required") String profileName,
        List<VlanRequest> vlans,
        List<SubnetRequest> subnets
) {
}
