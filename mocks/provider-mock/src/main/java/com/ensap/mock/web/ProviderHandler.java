package com.ensap.mock.web;

import com.ensap.mock.domain.ProviderType;
import com.ensap.mock.dto.ApiError;
import com.ensap.mock.service.ProviderSimulationService;
import com.ensap.mock.service.SimulationOutcome;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Translates HTTP requests for the {@code /api/v1/{providerType}/jobs}
 * surface every worker (router/switch/wireless/firewall/ticketing) calls
 * (master spec §22). All behavior logic lives in
 * {@link ProviderSimulationService}; this class only shapes HTTP.
 */
@Component
public class ProviderHandler {

    private final ProviderSimulationService simulationService;

    public ProviderHandler(ProviderSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> createJob(ServerRequest request) {
        ProviderType type = ProviderType.fromPath(request.pathVariable("providerType"));
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        if (type == null) {
            return badRequest(request, correlationId);
        }
        return request.bodyToMono(Map.class)
                .map(body -> (Map<String, Object>) body)
                .defaultIfEmpty(Map.of())
                .flatMap(body -> simulationService.simulate(type, body))
                .flatMap(outcome -> toResponse(outcome, correlationId));
    }

    private Mono<ServerResponse> toResponse(SimulationOutcome outcome, String correlationId) {
        if (outcome instanceof SimulationOutcome.Success success) {
            return ServerResponse.status(200).contentType(MediaType.APPLICATION_JSON).bodyValue(success.body());
        }
        SimulationOutcome.Failure failure = (SimulationOutcome.Failure) outcome;
        return ServerResponse.status(failure.status()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(failure.status(), failure.code(), failure.message(), correlationId));
    }

    private Mono<ServerResponse> badRequest(ServerRequest request, String correlationId) {
        String path = request.pathVariable("providerType");
        return ServerResponse.status(400).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiError.of(400, "UNKNOWN_PROVIDER", "Unknown provider type: " + path, correlationId));
    }
}
