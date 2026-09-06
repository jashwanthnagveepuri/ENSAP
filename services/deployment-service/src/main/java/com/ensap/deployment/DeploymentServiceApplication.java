package com.ensap.deployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deployment Service — owns deployment/batch/step lifecycle, idempotency
 * keys, and the outbox (docs/06-component-design.md). Phase 0: scaffolding
 * only, see config.RouterConfig for the (stubbed) HTTP API.
 */
@SpringBootApplication
public class DeploymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeploymentServiceApplication.class, args);
    }
}
