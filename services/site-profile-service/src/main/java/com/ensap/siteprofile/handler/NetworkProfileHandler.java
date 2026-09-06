package com.ensap.siteprofile.handler;

import com.ensap.siteprofile.dto.NetworkProfileRequest;
import com.ensap.siteprofile.exception.ErrorMapper;
import com.ensap.siteprofile.exception.RequestValidator;
import com.ensap.siteprofile.service.NetworkProfileService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** Translates HTTP requests/responses for network-profile endpoints nested under a site. */
@Component
public class NetworkProfileHandler {

    private final NetworkProfileService networkProfileService;
    private final RequestValidator validator;

    public NetworkProfileHandler(NetworkProfileService networkProfileService, RequestValidator validator) {
        this.networkProfileService = networkProfileService;
        this.validator = validator;
    }

    public Mono<ServerResponse> listProfiles(ServerRequest request) {
        String correlationId = correlationId(request);
        return networkProfileService.listProfiles(request.pathVariable("siteId"))
                .flatMap(profiles -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(profiles))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> getProfile(ServerRequest request) {
        String correlationId = correlationId(request);
        return networkProfileService.getProfile(request.pathVariable("siteId"), request.pathVariable("profileId"))
                .flatMap(profile -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(profile))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> createProfile(ServerRequest request) {
        String correlationId = correlationId(request);
        String siteId = request.pathVariable("siteId");
        return request.bodyToMono(NetworkProfileRequest.class)
                .map(validator::validate)
                .flatMap(body -> networkProfileService.createProfile(siteId, body))
                .flatMap(profile -> ServerResponse.status(201).contentType(MediaType.APPLICATION_JSON).bodyValue(profile))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> updateProfile(ServerRequest request) {
        String correlationId = correlationId(request);
        String siteId = request.pathVariable("siteId");
        String profileId = request.pathVariable("profileId");
        return request.bodyToMono(NetworkProfileRequest.class)
                .map(validator::validate)
                .flatMap(body -> networkProfileService.updateProfile(siteId, profileId, body))
                .flatMap(profile -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(profile))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> deleteProfile(ServerRequest request) {
        String correlationId = correlationId(request);
        return networkProfileService.deleteProfile(request.pathVariable("siteId"), request.pathVariable("profileId"))
                .then(ServerResponse.noContent().build())
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    private String correlationId(ServerRequest request) {
        return request.headers().firstHeader("X-Correlation-Id");
    }
}
