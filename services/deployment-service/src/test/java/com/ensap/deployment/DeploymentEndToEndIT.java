package com.ensap.deployment;

import com.ensap.deployment.dto.CreateDeploymentRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * End-to-end coverage of the Phase 2 checkpoint (master spec): "a deployment
 * can be created and tracked through a basic workflow" — driven through the
 * real HTTP router/handler/service/repository stack against Testcontainers
 * Postgres. Camunda is disabled (see AbstractIntegrationTest); the
 * create/retry/cancel state machine still runs, just via the documented
 * DISABLED outcome instead of a live Zeebe broker.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DeploymentEndToEndIT extends AbstractIntegrationTest {

    private static String deploymentId;

    @Autowired
    private WebTestClient client;

    @Test
    @Order(1)
    void create_thenIdempotentReplay_thenGetAndList() {
        client.post().uri("/api/deployments")
                .header("Idempotency-Key", "idem-key-1")
                .bodyValue(new CreateDeploymentRequest("SITE-1", "alice"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").value(id -> deploymentId = (String) id)
                .jsonPath("$.siteId").isEqualTo("SITE-1")
                .jsonPath("$.status").isEqualTo("REQUESTED")
                .jsonPath("$.steps[0].stepName").isEqualTo("camunda-workflow-start")
                .jsonPath("$.steps[0].status").isEqualTo("PENDING");

        // Replaying the same Idempotency-Key must return the SAME deployment (200, not 201), no duplicate.
        client.post().uri("/api/deployments")
                .header("Idempotency-Key", "idem-key-1")
                .bodyValue(new CreateDeploymentRequest("SITE-1", "someone-else"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.requestedBy").isEqualTo("alice"); // unchanged — the original, not the replayed body

        client.get().uri(uriBuilder -> uriBuilder.path("/api/deployments")
                        .queryParam("siteId", "SITE-1").queryParam("status", "REQUESTED").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1);
    }

    @Test
    @Order(2)
    void retry_whenNotFailed_returns409() {
        client.post().uri("/api/deployments/{id}/retry", deploymentId)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("CONFLICT");
    }

    @Test
    @Order(3)
    void cancel_fromRequested_transitionsToCancelled_thenSecondCancelConflicts() {
        String id = deploymentId;
        client.post().uri("/api/deployments/{id}/cancel", id)
                .exchange()
                .expectStatus().isEqualTo(202)
                .expectBody()
                .jsonPath("$.status").isEqualTo("CANCELLED");

        client.post().uri("/api/deployments/{id}/cancel", id)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void createDeployment_missingIdempotencyKey_returns400() {
        client.post().uri("/api/deployments")
                .bodyValue(new CreateDeploymentRequest("SITE-2", null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void createDeployment_missingSiteId_returns400ValidationError() {
        client.post().uri("/api/deployments")
                .header("Idempotency-Key", "idem-key-invalid")
                .bodyValue(new CreateDeploymentRequest(null, null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void getDeployment_unknownId_returns404() {
        client.get().uri("/api/deployments/{id}", "DEP-DOES-NOT-EXIST")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("DEPLOYMENT_NOT_FOUND");
    }
}
