package com.ensap.siteprofile.handler;

import com.ensap.siteprofile.exception.ApiError;
import com.ensap.siteprofile.service.SiteService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Translates HTTP requests/responses for site endpoints (docs/06-component-design.md
 * — Handler layer). Delegates business logic to {@link SiteService}; Phase 0
 * turns the service's "not implemented yet" signal into a 501 response with
 * the shared {@link ApiError} shape (docs/10-api-design.md) instead of a raw
 * 500, so the API contract is already correct even though behavior isn't.
 */
@Component
public class SiteHandler {

    private final SiteService siteService;

    public SiteHandler(SiteService siteService) {
        this.siteService = siteService;
    }

    public Mono<ServerResponse> listSites(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> getSite(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> refreshSite(ServerRequest request) {
        return notImplemented(request);
    }

    private Mono<ServerResponse> notImplemented(ServerRequest request) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(501)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.notImplemented(correlationId));
    }
}
