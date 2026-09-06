package com.ensap.mock.dto;

import java.time.Instant;

/** Standard error body for every non-2xx response (docs/10-api-design.md, master spec §31). */
public record ApiError(Instant timestamp, int status, String code, String message, String correlationId) {
    public static ApiError of(int status, String code, String message, String correlationId) {
        return new ApiError(Instant.now(), status, code, message, correlationId);
    }
}
