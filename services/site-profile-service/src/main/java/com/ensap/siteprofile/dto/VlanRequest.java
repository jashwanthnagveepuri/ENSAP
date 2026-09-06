package com.ensap.siteprofile.dto;

import jakarta.validation.constraints.NotNull;

public record VlanRequest(@NotNull(message = "vlanTag is required") Integer vlanTag, String name) {
}
