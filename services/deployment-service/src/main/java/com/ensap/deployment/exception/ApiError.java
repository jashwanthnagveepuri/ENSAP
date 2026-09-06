package com.ensap.deployment.exception;

import java.time.Instant;

/**
 * Standard error body for every API response (docs/10-api-design.md, master
 * spec §31). All handlers in this service return this shape on non-2xx.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String correlationId
) {
    public static ApiError of(int status, String code, String message, String correlationId) {
        return new ApiError(Instant.now(), status, code, message, correlationId);
    }
}
