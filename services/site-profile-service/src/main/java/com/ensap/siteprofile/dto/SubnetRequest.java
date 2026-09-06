package com.ensap.siteprofile.dto;

import jakarta.validation.constraints.NotBlank;

public record SubnetRequest(@NotBlank(message = "cidr is required") String cidr, String purpose) {
}
