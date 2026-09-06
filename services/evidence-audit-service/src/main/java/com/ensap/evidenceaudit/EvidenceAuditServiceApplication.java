package com.ensap.evidenceaudit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Evidence/Audit Service — owns deployment evidence, audit events, and
 * processed-operation records (docs/06-component-design.md). Phase 0:
 * scaffolding only, see config.RouterConfig for the (stubbed) HTTP API.
 */
@SpringBootApplication
public class EvidenceAuditServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvidenceAuditServiceApplication.class, args);
    }
}
