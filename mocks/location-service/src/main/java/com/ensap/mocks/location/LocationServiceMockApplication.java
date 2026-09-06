package com.ensap.mocks.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Mock location-service (Phase 1, master spec §22) — synthetic source of
 * truth for site existence/location fields that site-profile-service's
 * refresh flow reconciles against. Behavior is controllable per-request via
 * {@code ?mode=success|error|timeout|notfound} (default {@code success}) so
 * resilience/warning paths in the caller are exercisable without a real
 * upstream.
 */
@SpringBootApplication
public class LocationServiceMockApplication {

    private static final String[] REGIONS = {"us-east", "us-west", "eu-central", "ap-south"};
    private static final String[] STATUSES = {"ACTIVE", "PENDING", "DECOMMISSIONED"};

    public static void main(String[] args) {
        SpringApplication.run(LocationServiceMockApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return route(GET("/sites/{siteId}"), request -> {
            String siteId = request.pathVariable("siteId");
            String mode = request.queryParam("mode").orElse("success");
            return switch (mode) {
                case "error" -> ServerResponse.status(500).bodyValue(Map.of("error", "location-service synthetic failure"));
                case "notfound" -> ServerResponse.notFound().build();
                case "timeout" -> Mono.delay(Duration.ofSeconds(5))
                        .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId)));
                default -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId));
            };
        });
    }

    private Map<String, Object> synthesize(String siteId) {
        int hash = Math.abs(siteId.hashCode());
        return Map.of(
                "siteId", siteId,
                "name", "Site " + siteId,
                "region", REGIONS[hash % REGIONS.length],
                "status", STATUSES[hash % STATUSES.length]
        );
    }
}
