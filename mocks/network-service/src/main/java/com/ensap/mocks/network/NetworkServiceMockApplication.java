package com.ensap.mocks.network;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Mock network-service (Phase 1, master spec §22) — synthetic
 * source-of-truth for network profiles (VLANs/subnets) and WAN circuits
 * that site-profile-service's refresh flow reconciles against. Same
 * controllable-behavior pattern as mocks/location-service:
 * {@code ?mode=success|error|timeout|notfound}.
 */
@SpringBootApplication
public class NetworkServiceMockApplication {

    private static final String[] CARRIERS = {"AT&T", "Verizon", "Lumen", "Zayo"};

    public static void main(String[] args) {
        SpringApplication.run(NetworkServiceMockApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return route(GET("/sites/{siteId}"), request -> {
            String siteId = request.pathVariable("siteId");
            String mode = request.queryParam("mode").orElse("success");
            return switch (mode) {
                case "error" -> ServerResponse.status(500).bodyValue(Map.of("error", "network-service synthetic failure"));
                case "notfound" -> ServerResponse.notFound().build();
                case "timeout" -> Mono.delay(Duration.ofSeconds(5))
                        .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId)));
                default -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId));
            };
        });
    }

    private Map<String, Object> synthesize(String siteId) {
        int hash = Math.abs(siteId.hashCode());
        int vlanTag = 100 + (hash % 900);

        Map<String, Object> vlan = Map.of("vlanTag", vlanTag, "name", "data");
        Map<String, Object> subnet = Map.of("cidr", "10." + (hash % 256) + ".0.0/24", "purpose", "data");
        Map<String, Object> profile = Map.of(
                "profileName", "default",
                "vlans", List.of(vlan),
                "subnets", List.of(subnet)
        );
        Map<String, Object> circuit = Map.of(
                "carrier", CARRIERS[hash % CARRIERS.length],
                "circuitId", "CKT-" + siteId,
                "bandwidthMbps", 100 * (1 + hash % 10)
        );
        return Map.of("siteId", siteId, "networkProfiles", List.of(profile), "wanCircuits", List.of(circuit));
    }
}
