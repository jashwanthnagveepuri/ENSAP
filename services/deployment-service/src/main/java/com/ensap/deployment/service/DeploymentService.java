package com.ensap.deployment.service;

import com.ensap.deployment.dto.DeploymentResponse;
import com.ensap.deployment.repository.DeploymentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Business logic for deployments (docs/06-component-design.md — Service
 * layer). Phase 0: method signatures only. {@code createDeployment} will
 * enforce the {@code Idempotency-Key} contract and request the Camunda
 * {@code deployment-process} (workflow/camunda/deployment-process.bpmn) —
 * implemented starting Phase 2 (docs/02-functional-requirements.md, FR-2).
 */
@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    public Flux<DeploymentResponse> listDeployments() {
        throw new UnsupportedOperationException("listDeployments: implemented in Phase 2");
    }

    public Mono<DeploymentResponse> createDeployment(String idempotencyKey) {
        throw new UnsupportedOperationException("createDeployment: implemented in Phase 2");
    }

    public Mono<DeploymentResponse> getDeployment(String deploymentId) {
        throw new UnsupportedOperationException("getDeployment: implemented in Phase 2");
    }

    public Mono<DeploymentResponse> retryDeployment(String deploymentId) {
        throw new UnsupportedOperationException("retryDeployment: implemented in Phase 5");
    }

    public Mono<DeploymentResponse> cancelDeployment(String deploymentId) {
        throw new UnsupportedOperationException("cancelDeployment: implemented in Phase 5");
    }
}
