package com.ensap.deployment.handler;

import com.ensap.deployment.exception.ApiError;
import com.ensap.deployment.service.DeploymentService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Translates HTTP requests/responses for deployment endpoints
 * (docs/06-component-design.md — Handler layer). Delegates business logic to
 * {@link DeploymentService}; Phase 0 turns the service's "not implemented
 * yet" signal into a 501 with the shared {@link ApiError} shape
 * (docs/10-api-design.md) instead of a raw 500.
 */
@Component
public class DeploymentHandler {

    private final DeploymentService deploymentService;

    public DeploymentHandler(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    public Mono<ServerResponse> listDeployments(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> createDeployment(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> getDeployment(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> retryDeployment(ServerRequest request) {
        return notImplemented(request);
    }

    public Mono<ServerResponse> cancelDeployment(ServerRequest request) {
        return notImplemented(request);
    }

    private Mono<ServerResponse> notImplemented(ServerRequest request) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(501)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.notImplemented(correlationId));
    }
}
