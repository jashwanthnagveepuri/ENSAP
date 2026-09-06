package com.ensap.siteprofile.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Shared call pattern for the Phase 1 mock "source of truth" systems
 * (mocks/location-service, mocks/inventory-service, mocks/network-service)
 * that {@code POST /api/sites/{siteId}/refresh} reconciles against
 * (master spec §22). A slow/erroring/unreachable source must never fail
 * the whole refresh — it just means that section of the site profile
 * keeps its previous data, surfaced as a warning to the caller.
 */
public abstract class SourceSystemClient {

    private static final Logger log = LoggerFactory.getLogger(SourceSystemClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    protected final WebClient webClient;
    private final String sourceName;

    protected SourceSystemClient(WebClient.Builder webClientBuilder, String baseUrl, String sourceName) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.sourceName = sourceName;
    }

    protected <T> Mono<Optional<T>> fetchOptional(String siteId, Class<T> type) {
        return webClient.get()
                .uri("/sites/{siteId}", siteId)
                .retrieve()
                .bodyToMono(type)
                .timeout(TIMEOUT)
                .map(Optional::of)
                .onErrorResume(ex -> {
                    log.warn("{} unavailable for site {}: {}", sourceName, siteId, ex.toString());
                    return Mono.just(Optional.empty());
                });
    }

    public String sourceName() {
        return sourceName;
    }
}
