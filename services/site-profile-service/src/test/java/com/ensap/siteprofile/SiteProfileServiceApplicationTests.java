package com.ensap.siteprofile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 0 smoke test: the Spring context must load (all router/handler/
 * service/repository beans wire up correctly). Uses the {@code test} profile
 * (application-test.yml) to point at an H2 in-memory database so this runs
 * without a live PostgreSQL instance — real persistence integration tests
 * (Testcontainers) are added starting Phase 1 (docs/17-testing-strategy.md).
 */
@SpringBootTest
@ActiveProfiles("test")
class SiteProfileServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
