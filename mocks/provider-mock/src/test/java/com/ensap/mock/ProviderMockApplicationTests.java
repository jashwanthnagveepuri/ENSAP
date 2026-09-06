package com.ensap.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test: the Spring context must load with no external dependencies (mock has none). */
@SpringBootTest
class ProviderMockApplicationTests {

    @Test
    void contextLoads() {
    }
}
