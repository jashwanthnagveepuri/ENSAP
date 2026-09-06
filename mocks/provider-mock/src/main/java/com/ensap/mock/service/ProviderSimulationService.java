package com.ensap.mock.service;

import com.ensap.mock.domain.ProviderBehavior;
import com.ensap.mock.domain.ProviderType;
import com.ensap.mock.domain.ResponseMode;
import com.ensap.mock.dto.JobResponse;
import com.ensap.mock.state.ProviderBehaviorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Applies a provider's controllable behavior (master spec §22) to one
 * incoming job request: latency, temporary outage, rate limiting, random
 * failure percentage, the explicit response mode, and — on success —
 * duplicate callback delivery. This is the only class that knows the
 * evaluation order; {@code ProviderHandler} just turns the outcome into an
 * HTTP response.
 */
@Service
public class ProviderSimulationService {

    private static final Logger log = LoggerFactory.getLogger(ProviderSimulationService.class);

    /** How long a TIMEOUT-mode request hangs before finally giving up — long enough that any
     *  real client-side timeout (master spec §21) fires first; this response is normally never seen. */
    private static final Duration TIMEOUT_DELAY = Duration.ofSeconds(65);

    private final ProviderBehaviorStore store;
    private final WebClient webClient;

    public ProviderSimulationService(ProviderBehaviorStore store, WebClient.Builder webClientBuilder) {
        this.store = store;
        this.webClient = webClientBuilder.build();
    }

    public Mono<SimulationOutcome> simulate(ProviderType type, Map<String, Object> requestBody) {
        ProviderBehavior behavior = store.get(type);

        return delayFor(behavior)
                .then(Mono.defer(() -> Mono.just(evaluate(type, behavior, requestBody))))
                .doOnNext(outcome -> maybeSendDuplicateCallback(type, behavior, requestBody, outcome));
    }

    private Mono<Void> delayFor(ProviderBehavior behavior) {
        if (behavior.responseMode() == ResponseMode.TIMEOUT) {
            return Mono.delay(TIMEOUT_DELAY).then();
        }
        long latencyMs = behavior.fixedLatencyMs();
        if (behavior.randomLatencyMaxMs() > 0) {
            long min = behavior.randomLatencyMinMs();
            long max = behavior.randomLatencyMaxMs();
            latencyMs += (min >= max) ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
        }
        return latencyMs > 0 ? Mono.delay(Duration.ofMillis(latencyMs)).then() : Mono.empty();
    }

    private SimulationOutcome evaluate(ProviderType type, ProviderBehavior behavior, Map<String, Object> requestBody) {
        if (behavior.outageEnabled()) {
            return failure(503, "PROVIDER_OUTAGE", type + " provider is simulating a temporary outage");
        }
        if (store.isRateLimited(type, behavior.rateLimitPerMinute())) {
            return failure(429, "RATE_LIMITED", "Rate limit of " + behavior.rateLimitPerMinute() + "/min exceeded for " + type);
        }
        if (behavior.randomFailurePercent() > 0
                && ThreadLocalRandom.current().nextInt(100) < behavior.randomFailurePercent()) {
            return failure(500, "RANDOM_FAILURE", type + " provider randomly failed this request (configured " + behavior.randomFailurePercent() + "%)");
        }
        ResponseMode mode = behavior.responseMode();
        if (mode == ResponseMode.TIMEOUT) {
            // Client should have already timed out during delayFor(); if not, surface it as such.
            return failure(504, "TIMEOUT", type + " provider simulated a timeout");
        }
        if (mode != ResponseMode.SUCCESS) {
            return failure(mode.httpStatus(), mode.name(), type + " provider is configured to return " + mode.httpStatus());
        }
        String jobId = UUID.randomUUID().toString();
        return new SimulationOutcome.Success(new JobResponse(jobId, type.name(), "COMPLETED", Instant.now(),
                "Simulated by provider-mock; request payload keys: " + requestBody.keySet()));
    }

    private SimulationOutcome.Failure failure(int status, String code, String message) {
        return new SimulationOutcome.Failure(status, code, message);
    }

    /** Fire-and-forget: on success, if duplicateCallback is on and the caller supplied a callbackUrl, POST it twice. */
    private void maybeSendDuplicateCallback(ProviderType type, ProviderBehavior behavior,
                                             Map<String, Object> requestBody, SimulationOutcome outcome) {
        if (!behavior.duplicateCallback() || !(outcome instanceof SimulationOutcome.Success success)) {
            return;
        }
        Object callbackUrl = requestBody.get("callbackUrl");
        if (!(callbackUrl instanceof String url) || url.isBlank()) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            webClient.post().uri(url)
                    .bodyValue(success.body())
                    .retrieve()
                    .toBodilessEntity()
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            r -> log.info("duplicate callback delivered to {} for {}", url, type),
                            e -> log.warn("duplicate callback to {} failed for {}: {}", url, type, e.toString()));
        }
    }
}
