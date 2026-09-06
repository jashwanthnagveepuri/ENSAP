package com.ensap.siteprofile.exception;

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
    public static ApiError notImplemented(String correlationId) {
        return new ApiError(
                Instant.now(),
                501,
                "NOT_IMPLEMENTED",
                "This endpoint is a Phase 0 scaffold stub; business logic lands in a later phase (see docs/README status).",
                correlationId
        );
    }
}
