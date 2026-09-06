package com.ensap.siteprofile.handler;

import com.ensap.siteprofile.dto.SiteRequest;
import com.ensap.siteprofile.exception.ErrorMapper;
import com.ensap.siteprofile.exception.RequestValidator;
import com.ensap.siteprofile.service.SiteService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Translates HTTP requests/responses for site endpoints (docs/06-component-design.md
 * — Handler layer). Delegates business logic to {@link SiteService}.
 */
@Component
public class SiteHandler {

    private final SiteService siteService;
    private final RequestValidator validator;

    public SiteHandler(SiteService siteService, RequestValidator validator) {
        this.siteService = siteService;
        this.validator = validator;
    }

    public Mono<ServerResponse> createSite(ServerRequest request) {
        String correlationId = correlationId(request);
        return request.bodyToMono(SiteRequest.class)
                .map(validator::validate)
                .flatMap(siteService::createSite)
                .flatMap(site -> ServerResponse.status(201).contentType(MediaType.APPLICATION_JSON).bodyValue(site))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> listSites(ServerRequest request) {
        String correlationId = correlationId(request);
        return Mono.defer(() -> siteService.searchSites(
                        request.queryParam("status").orElse(null),
                        request.queryParam("region").orElse(null),
                        request.queryParam("name").orElse(null),
                        request.queryParam("page").map(Integer::parseInt).orElse(0),
                        request.queryParam("size").map(Integer::parseInt).orElse(20)))
                .flatMap(result -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> getSite(ServerRequest request) {
        String correlationId = correlationId(request);
        return siteService.getSite(request.pathVariable("siteId"))
                .flatMap(site -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(site))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> updateSite(ServerRequest request) {
        String correlationId = correlationId(request);
        String siteId = request.pathVariable("siteId");
        return request.bodyToMono(SiteRequest.class)
                .map(validator::validate)
                .flatMap(body -> siteService.updateSite(siteId, body))
                .flatMap(site -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(site))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> deleteSite(ServerRequest request) {
        String correlationId = correlationId(request);
        return siteService.deleteSite(request.pathVariable("siteId"))
                .then(ServerResponse.noContent().build())
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> refreshSite(ServerRequest request) {
        String correlationId = correlationId(request);
        return siteService.refreshSite(request.pathVariable("siteId"))
                .flatMap(result -> ServerResponse.status(202).contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new RefreshResponseBody(result.detail(), result.warnings())))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    private String correlationId(ServerRequest request) {
        return request.headers().firstHeader("X-Correlation-Id");
    }

    private record RefreshResponseBody(com.ensap.siteprofile.dto.SiteDetailResponse site, java.util.List<String> warnings) {
    }
}
