package com.ensap.deployment.handler;

import com.ensap.deployment.dto.CreateDeploymentRequest;
import com.ensap.deployment.exception.ErrorMapper;
import com.ensap.deployment.exception.RequestValidator;
import com.ensap.deployment.exception.ValidationException;
import com.ensap.deployment.service.DeploymentService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Translates HTTP requests/responses for deployment endpoints
 * (docs/06-component-design.md — Handler layer). Delegates business logic to
 * {@link DeploymentService}.
 */
@Component
public class DeploymentHandler {

    private final DeploymentService deploymentService;
    private final RequestValidator validator;

    public DeploymentHandler(DeploymentService deploymentService, RequestValidator validator) {
        this.deploymentService = deploymentService;
        this.validator = validator;
    }

    public Mono<ServerResponse> createDeployment(ServerRequest request) {
        String correlationId = correlationId(request);
        String idempotencyKey = request.headers().firstHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ErrorMapper.toResponse(
                    new ValidationException(List.of("Idempotency-Key header is required")), correlationId);
        }
        return request.bodyToMono(CreateDeploymentRequest.class)
                .map(validator::validate)
                .flatMap(body -> deploymentService.createDeployment(body, idempotencyKey))
                .flatMap(result -> ServerResponse.status(result.created() ? 201 : 200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result.deployment()))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> listDeployments(ServerRequest request) {
        String correlationId = correlationId(request);
        return Mono.defer(() -> deploymentService.listDeployments(
                        request.queryParam("status").orElse(null),
                        request.queryParam("siteId").orElse(null),
                        request.queryParam("page").map(Integer::parseInt).orElse(0),
                        request.queryParam("size").map(Integer::parseInt).orElse(20)))
                .flatMap(result -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(result))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> getDeployment(ServerRequest request) {
        String correlationId = correlationId(request);
        return deploymentService.getDeployment(request.pathVariable("deploymentId"))
                .flatMap(deployment -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(deployment))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> retryDeployment(ServerRequest request) {
        String correlationId = correlationId(request);
        return deploymentService.retryDeployment(request.pathVariable("deploymentId"))
                .flatMap(deployment -> ServerResponse.status(202).contentType(MediaType.APPLICATION_JSON).bodyValue(deployment))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> cancelDeployment(ServerRequest request) {
        String correlationId = correlationId(request);
        return deploymentService.cancelDeployment(request.pathVariable("deploymentId"))
                .flatMap(deployment -> ServerResponse.status(202).contentType(MediaType.APPLICATION_JSON).bodyValue(deployment))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    private String correlationId(ServerRequest request) {
        return request.headers().firstHeader("X-Correlation-Id");
    }
}
