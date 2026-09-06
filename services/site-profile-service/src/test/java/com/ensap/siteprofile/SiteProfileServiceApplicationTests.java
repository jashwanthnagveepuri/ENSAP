package com.ensap.siteprofile;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: the Spring context must load against real PostgreSQL/Redis
 * (Testcontainers, {@link AbstractIntegrationTest}) — every router/handler/
 * service/repository bean wires up and Liquibase applies the changelog
 * cleanly.
 */
class SiteProfileServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
