package com.ensap.deployment.exception;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Single place every handler routes exceptions through so the error model
 * (docs/10-api-design.md §31) stays consistent without repeating the
 * instanceof chain in each handler method.
 */
public final class ErrorMapper {

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
        } else {
            status = 500;
            code = "INTERNAL_ERROR";
            message = "Unexpected error";
        }

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(status, code, message, correlationId));
    }
}
