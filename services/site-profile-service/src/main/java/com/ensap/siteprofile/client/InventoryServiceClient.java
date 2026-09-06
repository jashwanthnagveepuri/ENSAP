package com.ensap.siteprofile.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Client for {@code mocks/inventory-service} — synthetic source-of-truth for
 * device inventory (master spec §22).
 */
@Component
public class InventoryServiceClient extends SourceSystemClient {

    public InventoryServiceClient(WebClient.Builder webClientBuilder,
                                   @Value("${site-profile.source-systems.inventory-url:http://localhost:8092}") String baseUrl) {
        super(webClientBuilder, baseUrl, "inventory-service");
    }

    public Mono<Optional<InventoryRecord>> fetchInventory(String siteId) {
        return fetchOptional(siteId, InventoryRecord.class);
    }

    public record InventoryRecord(String siteId, List<DeviceRecord> devices) {
    }

    public record DeviceRecord(String type, String vendor, String model, String serialNumber, String status) {
    }
}
