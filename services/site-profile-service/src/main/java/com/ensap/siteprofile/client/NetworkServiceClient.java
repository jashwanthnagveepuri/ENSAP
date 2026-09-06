package com.ensap.siteprofile.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Client for {@code mocks/network-service} — synthetic source-of-truth for
 * network profiles (VLANs/subnets) and WAN circuits (master spec §22).
 */
@Component
public class NetworkServiceClient extends SourceSystemClient {

    public NetworkServiceClient(WebClient.Builder webClientBuilder,
                                 @Value("${site-profile.source-systems.network-url:http://localhost:8093}") String baseUrl) {
        super(webClientBuilder, baseUrl, "network-service");
    }

    public Mono<Optional<NetworkRecord>> fetchNetwork(String siteId) {
        return fetchOptional(siteId, NetworkRecord.class);
    }

    public record NetworkRecord(String siteId, List<NetworkProfileRecord> networkProfiles,
                                 List<WanCircuitRecord> wanCircuits) {
    }

    public record NetworkProfileRecord(String profileName, List<VlanRecord> vlans, List<SubnetRecord> subnets) {
    }

    public record VlanRecord(Integer vlanTag, String name) {
    }

    public record SubnetRecord(String cidr, String purpose) {
    }

    public record WanCircuitRecord(String carrier, String circuitId, Integer bandwidthMbps) {
    }
}
