package com.ensap.deployment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Single place every handler routes exceptions through so the error model
 * (docs/10-api-design.md §31) stays consistent without repeating the
 * instanceof chain in each handler method.
 */
public final class ErrorMapper {

    private static final Logger log = LoggerFactory.getLogger(ErrorMapper.class);

    private ErrorMapper() {
    }

    public static Mono<ServerResponse> toResponse(Throwable ex, String correlationId) {
        int status;
        String code;
        String message = ex.getMessage();

        if (ex instanceof NotFoundException nfe) {
            status = 404;
            code = nfe.getCode();
        } else if (ex instanceof ConflictException) {
            status = 409;
            code = "CONFLICT";
        } else if (ex instanceof ValidationException) {
            status = 400;
            code = "VALIDATION_ERROR";
        } else if (ex instanceof IllegalArgumentException) {
            status = 400;
            code = "BAD_REQUEST";
        } else if (ex instanceof ResponseStatusException rse) {
            // e.g. wrong Content-Type or malformed body, raised by the framework before our code runs.
            status = rse.getStatusCode().value();
            code = "BAD_REQUEST";
        } else {
            status = 500;
            code = "INTERNAL_ERROR";
            message = "Unexpected error";
            log.error("Unhandled exception (correlationId={})", correlationId, ex);
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(status, code, message, correlationId));
    }
}
