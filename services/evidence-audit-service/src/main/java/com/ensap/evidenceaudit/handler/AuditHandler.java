package com.ensap.evidenceaudit.handler;

import com.ensap.evidenceaudit.exception.ApiError;
import com.ensap.evidenceaudit.service.AuditService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Translates HTTP requests/responses for audit endpoints
 * (docs/06-component-design.md — Handler layer). Delegates to
 * {@link AuditService}; Phase 0 returns 501 with the shared
 * {@link ApiError} shape (docs/10-api-design.md).
 */
@Component
public class AuditHandler {

    private final AuditService auditService;

    public AuditHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    public Mono<ServerResponse> listAuditEvents(ServerRequest request) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(501)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.notImplemented(correlationId));
    }
}
