package com.ensap.deployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Deployment Service — owns deployment/batch/step lifecycle, idempotency
 * keys, and the outbox (docs/06-component-design.md). {@code @EnableScheduling}
 * drives {@code outbox.OutboxPublisher} (master spec §11, §37 Phase 3).
 */
@SpringBootApplication
@EnableScheduling
public class DeploymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeploymentServiceApplication.class, args);
    }
}
