package com.ensap.mock.web;

import com.ensap.mock.domain.ProviderBehavior;
import com.ensap.mock.domain.ProviderType;
import com.ensap.mock.dto.ApiError;
import com.ensap.mock.state.ProviderBehaviorStore;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Provider-Simulation admin API (master spec §22): lets the operator
 * console change a mock provider's behavior at runtime, e.g. mid-demo.
 */
@Component
public class AdminHandler {

    private final ProviderBehaviorStore store;

    public AdminHandler(ProviderBehaviorStore store) {
        this.store = store;
    }

    public Mono<ServerResponse> listAll(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(store.snapshotAll());
    }

    public Mono<ServerResponse> getOne(ServerRequest request) {
        ProviderType type = ProviderType.fromPath(request.pathVariable("providerType"));
        if (type == null) {
            return unknownProvider(request);
        }
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(store.get(type));
    }

    public Mono<ServerResponse> update(ServerRequest request) {
        ProviderType type = ProviderType.fromPath(request.pathVariable("providerType"));
        if (type == null) {
            return unknownProvider(request);
        }
        return request.bodyToMono(ProviderBehavior.class)
                .flatMap(behavior -> {
                    String validationError = behavior.validate();
                    if (validationError != null) {
                        return badRequest(request, validationError);
                    }
                    store.set(type, behavior);
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(store.get(type));
                });
    }

    public Mono<ServerResponse> reset(ServerRequest request) {
        ProviderType type = ProviderType.fromPath(request.pathVariable("providerType"));
        if (type == null) {
            return unknownProvider(request);
        }
        store.reset(type);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(store.get(type));
    }

    private Mono<ServerResponse> unknownProvider(ServerRequest request) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(404).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(404, "UNKNOWN_PROVIDER", "Unknown provider type: " + request.pathVariable("providerType"), correlationId));
    }

    private Mono<ServerResponse> badRequest(ServerRequest request, String message) {
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        return ServerResponse.status(400).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(400, "INVALID_BEHAVIOR", message, correlationId));
    }
}
