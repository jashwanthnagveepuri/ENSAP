package com.ensap.evidenceaudit.handler;

import com.ensap.evidenceaudit.exception.ApiError;
import com.ensap.evidenceaudit.service.EvidenceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Translates HTTP requests/responses for evidence endpoints
 * (docs/06-component-design.md — Handler layer). Delegates to
 * {@link EvidenceService}; Phase 0 returns 501 with the shared
 * {@link ApiError} shape (docs/10-api-design.md).
 */
@Component
public class EvidenceHandler {

    private final EvidenceService evidenceService;

    public EvidenceHandler(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    public Mono<ServerResponse> listEvidence(ServerRequest request) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(501)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.notImplemented(correlationId));
    }
}
