package com.ensap.mock.web;

import com.ensap.mock.domain.ProviderBehavior;
import com.ensap.mock.domain.ResponseMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

/**
 * Exercises every controllable-behavior knob (master spec §22) end to end
 * through the real HTTP routes: each test owns a distinct provider type so
 * they can run without resetting shared state between each other.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderBehaviorTest {

    @Autowired
    private WebTestClient client;

    @Test
    void defaultBehaviorIsSuccess() {
        postJob("router")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
                .jsonPath("$.providerType").isEqualTo("ROUTER");
    }

    @Test
    void unknownProviderTypeIsBadRequest() {
        postJob("not-a-provider").expectStatus().isBadRequest();
    }

    @Test
    void adminCanForceAnErrorResponseMode() {
        setBehavior("switch", new ProviderBehavior(ResponseMode.NOT_FOUND, 0, 0, 0, false, 0, false, 0));

        postJob("switch")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void outageForcesServiceUnavailableRegardlessOfResponseMode() {
        setBehavior("wireless", new ProviderBehavior(ResponseMode.SUCCESS, 0, 0, 0, true, 0, false, 0));

        postJob("wireless")
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("PROVIDER_OUTAGE");
    }

    @Test
    void randomFailureAt100PercentAlwaysFails() {
        setBehavior("firewall", new ProviderBehavior(ResponseMode.SUCCESS, 0, 0, 0, false, 100, false, 0));

        postJob("firewall")
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("RANDOM_FAILURE");
    }

    @Test
    void rateLimitRejectsRequestsPastThePerMinuteCap() {
        setBehavior("ticketing", new ProviderBehavior(ResponseMode.SUCCESS, 0, 0, 0, false, 0, false, 1));

        postJob("ticketing").expectStatus().isOk();
        postJob("ticketing")
                .expectStatus().isEqualTo(429)
                .expectBody()
                .jsonPath("$.code").isEqualTo("RATE_LIMITED");
    }

    @Test
    void adminRejectsInvalidBehavior() {
        client.put().uri("/admin/providers/router")
                .bodyValue(new ProviderBehavior(ResponseMode.SUCCESS, -1, 0, 0, false, 0, false, 0))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void adminResetRestoresDefaults() {
        setBehavior("router", new ProviderBehavior(ResponseMode.BAD_REQUEST, 0, 0, 0, false, 0, false, 0));

        client.post().uri("/admin/providers/router/reset")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.responseMode").isEqualTo("SUCCESS");
    }

    private ResponseSpec postJob(String providerType) {
        return client.post().uri("/api/v1/{providerType}/jobs", providerType)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange();
    }

    private void setBehavior(String providerType, ProviderBehavior behavior) {
        client.put().uri("/admin/providers/{providerType}", providerType)
                .bodyValue(behavior)
                .exchange()
                .expectStatus().isOk();
    }
}
