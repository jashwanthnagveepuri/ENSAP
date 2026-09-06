package com.ensap.siteprofile;

import com.ensap.siteprofile.dto.SiteDetailResponse;
import com.ensap.siteprofile.dto.SiteRequest;
import com.ensap.siteprofile.dto.SiteResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the Phase 1 checkpoint (master spec): "a user can
 * create/search/view/refresh a synthetic site profile" — driven through the
 * real HTTP router/handler/service/repository stack against Testcontainers
 * Postgres/Redis, with the three source-system calls pointed at in-JVM
 * reactor-netty stubs instead of the real mocks/* processes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SiteProfileEndToEndIT extends AbstractIntegrationTest {

    private static final DisposableServer LOCATION_SERVER = stubServer(
            "{\"siteId\":\"%s\",\"name\":\"Refreshed Name\",\"region\":\"eu-central\",\"status\":\"ACTIVE\"}");
    private static final DisposableServer INVENTORY_SERVER = stubServer(
            "{\"siteId\":\"%s\",\"devices\":[{\"type\":\"ROUTER\",\"vendor\":\"Cisco\",\"model\":\"ISR-1000\","
                    + "\"serialNumber\":\"SN-1\",\"status\":\"ACTIVE\"}]}");
    private static final DisposableServer NETWORK_SERVER = stubServer(
            "{\"siteId\":\"%s\",\"networkProfiles\":[{\"profileName\":\"default\",\"vlans\":[{\"vlanTag\":10,"
                    + "\"name\":\"data\"}],\"subnets\":[{\"cidr\":\"10.0.0.0/24\",\"purpose\":\"data\"}]}],"
                    + "\"wanCircuits\":[{\"carrier\":\"AT&T\",\"circuitId\":\"CKT-1\",\"bandwidthMbps\":500}]}");

    private static String siteId;

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void sourceSystemUrls(DynamicPropertyRegistry registry) {
        registry.add("site-profile.source-systems.location-url", () -> "http://localhost:" + LOCATION_SERVER.port());
        registry.add("site-profile.source-systems.inventory-url", () -> "http://localhost:" + INVENTORY_SERVER.port());
        registry.add("site-profile.source-systems.network-url", () -> "http://localhost:" + NETWORK_SERVER.port());
    }

    @AfterAll
    static void stopServers() {
        LOCATION_SERVER.disposeNow();
        INVENTORY_SERVER.disposeNow();
        NETWORK_SERVER.disposeNow();
    }

    private static DisposableServer stubServer(String bodyTemplate) {
        return HttpServer.create().port(0)
                .route(routes -> routes.get("/sites/{siteId}", (req, res) -> res
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just(String.format(bodyTemplate, req.param("siteId"))))))
                .bindNow();
    }

    @Test
    @Order(1)
    void create_thenSearch_thenView() {
        SiteRequest request = new SiteRequest(null, "Denver Branch", "us-west", "ACTIVE", null);

        SiteResponse created = client.post().uri("/api/sites")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SiteResponse.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id()).startsWith("SITE-");
        siteId = created.id();

        client.get().uri(uriBuilder -> uriBuilder.path("/api/sites").queryParam("status", "ACTIVE").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content[?(@.id=='" + siteId + "')]").exists();

        client.get().uri("/api/sites/{id}", siteId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SiteDetailResponse.class)
                .value(detail -> assertThat(detail.name()).isEqualTo("Denver Branch"));
    }

    @Test
    @Order(2)
    void refresh_reconcilesFromSourceSystemsWithNoWarnings() {
        client.post().uri("/api/sites/{id}/refresh", siteId)
                .exchange()
                .expectStatus().isEqualTo(202)
                .expectBody()
                .jsonPath("$.warnings").isEmpty()
                .jsonPath("$.site.name").isEqualTo("Refreshed Name")
                .jsonPath("$.site.devices[0].vendor").isEqualTo("Cisco")
                .jsonPath("$.site.networkProfiles[0].vlans[0].vlanTag").isEqualTo(10)
                .jsonPath("$.site.wanCircuits[0].carrier").isEqualTo("AT&T");

        client.get().uri("/api/sites/{id}", siteId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SiteDetailResponse.class)
                .value(detail -> {
                    assertThat(detail.name()).isEqualTo("Refreshed Name");
                    assertThat(detail.devices()).hasSize(1);
                    assertThat(detail.networkProfiles()).hasSize(1);
                    assertThat(detail.wanCircuits()).hasSize(1);
                });
    }

    @Test
    @Order(3)
    void delete_thenViewReturnsNotFoundWithApiErrorShape() {
        client.delete().uri("/api/sites/{id}", siteId)
                .exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/sites/{id}", siteId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("SITE_NOT_FOUND")
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void createSite_missingName_returns400ValidationError() {
        SiteRequest invalid = new SiteRequest(null, "  ", "us-west", "ACTIVE", null);

        client.post().uri("/api/sites")
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }
}
