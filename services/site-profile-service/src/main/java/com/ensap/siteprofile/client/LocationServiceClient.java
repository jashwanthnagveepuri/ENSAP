package com.ensap.siteprofile.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Client for {@code mocks/location-service} — synthetic source-of-truth for
 * site existence/location fields (master spec §22).
 */
@Component
public class LocationServiceClient extends SourceSystemClient {

    public LocationServiceClient(WebClient.Builder webClientBuilder,
                                  @Value("${site-profile.source-systems.location-url:http://localhost:8091}") String baseUrl) {
        super(webClientBuilder, baseUrl, "location-service");
    }

    public Mono<Optional<LocationRecord>> fetchLocation(String siteId) {
        return fetchOptional(siteId, LocationRecord.class);
    }

    public record LocationRecord(String siteId, String name, String region, String status) {
    }
}
