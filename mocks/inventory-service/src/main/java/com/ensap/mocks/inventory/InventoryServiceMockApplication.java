package com.ensap.mocks.inventory;

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
 * Mock inventory-service (Phase 1, master spec §22) — synthetic
 * source-of-truth for device inventory that site-profile-service's refresh
 * flow reconciles against. Same controllable-behavior pattern as
 * mocks/location-service: {@code ?mode=success|error|timeout|notfound}.
 */
@SpringBootApplication
public class InventoryServiceMockApplication {

    private static final String[] TYPES = {"ROUTER", "SWITCH", "FIREWALL", "WIRELESS_AP"};
    private static final String[] VENDORS = {"Cisco", "Juniper", "Aruba", "Palo Alto"};

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceMockApplication.class, args);
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return route(GET("/sites/{siteId}"), request -> {
            String siteId = request.pathVariable("siteId");
            String mode = request.queryParam("mode").orElse("success");
            return switch (mode) {
                case "error" -> ServerResponse.status(500).bodyValue(Map.of("error", "inventory-service synthetic failure"));
                case "notfound" -> ServerResponse.notFound().build();
                case "timeout" -> Mono.delay(Duration.ofSeconds(5))
                        .then(ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId)));
                default -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(synthesize(siteId));
            };
        });
    }

    private Map<String, Object> synthesize(String siteId) {
        int hash = Math.abs(siteId.hashCode());
        int deviceCount = 2 + (hash % 3);
        List<Map<String, Object>> devices = new java.util.ArrayList<>();
        for (int i = 0; i < deviceCount; i++) {
            int idx = (hash + i) % TYPES.length;
            devices.add(Map.of(
                    "type", TYPES[idx],
                    "vendor", VENDORS[idx],
                    "model", TYPES[idx] + "-" + (100 + i),
                    "serialNumber", "SN-" + siteId + "-" + i,
                    "status", "ACTIVE"
            ));
        }
        return Map.of("siteId", siteId, "devices", devices);
    }
}
