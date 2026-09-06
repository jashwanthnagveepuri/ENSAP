package com.ensap.evidenceaudit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 0 smoke test: the Spring context must load. Uses the {@code test}
 * profile (application-test.yml) to point at an H2 in-memory database so
 * this runs without a live PostgreSQL instance — real persistence
 * integration tests (Testcontainers) are added starting Phase 2
 * (docs/17-testing-strategy.md).
 */
@SpringBootTest
@ActiveProfiles("test")
class EvidenceAuditServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
