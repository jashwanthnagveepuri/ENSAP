package com.ensap.deployment;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for every test that needs a real Spring context — starts real
 * PostgreSQL via Testcontainers instead of H2 (docs/17-testing-strategy.md,
 * mirrors site-profile-service's AbstractIntegrationTest). Camunda is
 * disabled here rather than run against a Zeebe testcontainer: the
 * create/retry/cancel state machine is exercised through the DISABLED
 * outcome (design-verified — see WorkflowServiceTest and README for the
 * documented rationale), while STARTED/ERROR outcomes are covered with a
 * mocked WorkflowService in DeploymentServiceTest.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "deployment.camunda.enabled=false")
@AutoConfigureWebTestClient(timeout = "10000")
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("ensap").withUsername("ensap").withPassword("ensap_dev_only");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
