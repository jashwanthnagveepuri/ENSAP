package com.ensap.deployment.service;

import com.ensap.deployment.dto.CreateDeploymentRequest;
import com.ensap.deployment.dto.DeploymentResponse;
import com.ensap.deployment.dto.DeploymentStepResponse;
import com.ensap.deployment.dto.PageResponse;
import com.ensap.deployment.entity.Deployment;
import com.ensap.deployment.entity.DeploymentStatus;
import com.ensap.deployment.entity.DeploymentStep;
import com.ensap.deployment.entity.DeploymentStepStatus;
import com.ensap.deployment.entity.IdempotencyKeyRecord;
import com.ensap.deployment.entity.OutboxEvent;
import com.ensap.deployment.exception.ConflictException;
import com.ensap.deployment.exception.NotFoundException;
import com.ensap.deployment.repository.DeploymentRepository;
import com.ensap.deployment.repository.DeploymentStepRepository;
import com.ensap.deployment.repository.IdempotencyKeyRepository;
import com.ensap.deployment.repository.OutboxEventRepository;
import com.ensap.deployment.util.IdGenerator;
import com.ensap.deployment.workflow.StartResult;
import com.ensap.deployment.workflow.WorkflowService;
import com.ensap.events.DeploymentEventPayload;
import com.ensap.events.DeploymentEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for deployments (docs/06-component-design.md — Service
 * layer; master spec §7.2/§12/§37 Phase 2). Repository calls are blocking
 * JPA, so every DB access runs on {@code boundedElastic} like
 * site-profile-service's {@code SiteService}.
 */
@Service
public class DeploymentService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentService.class);

    /** After this many failed workflow-start attempts, escalate to FAILED_REQUIRES_ATTENTION. */
    private static final int MAX_ATTEMPTS_BEFORE_ESCALATION = 3;

    private final DeploymentRepository deploymentRepository;
    private final DeploymentStepRepository stepRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final WorkflowService workflowService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public DeploymentService(DeploymentRepository deploymentRepository, DeploymentStepRepository stepRepository,
                              IdempotencyKeyRepository idempotencyKeyRepository, OutboxEventRepository outboxEventRepository,
                              WorkflowService workflowService, PlatformTransactionManager transactionManager,
                              ObjectMapper objectMapper) {
        this.deploymentRepository = deploymentRepository;
        this.stepRepository = stepRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.workflowService = workflowService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
    }

    /**
     * Writes the outbox_event row for a deployment state change in the SAME
     * transaction as the change itself (ADR-0004 — no dual-write). Every call
     * site below already runs inside {@code transactionTemplate}.
     */
    private void writeOutboxEvent(Deployment deployment, DeploymentEventType eventType, String errorMessage) {
        try {
            DeploymentEventPayload payload = new DeploymentEventPayload(deployment.getId(), deployment.getSiteId(),
                    deployment.getStatus(), deployment.getRequestedBy(), errorMessage);
            OutboxEvent event = new OutboxEvent();
            event.setId(UUID.randomUUID());
            event.setAggregateType("deployment");
            event.setAggregateId(deployment.getId());
            event.setEventType(eventType.type());
            event.setEventVersion(1);
            event.setPayload(objectMapper.writeValueAsString(payload));
            // ponytail: a fresh correlationId per event, not one shared across a deployment's whole
            // lifecycle — proper request-scoped correlation/trace propagation arrives with
            // common-observability adoption (§24, Phase 8); the envelope field is populated either way.
            event.setCorrelationId(UUID.randomUUID());
            event.setCreatedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload for deployment " + deployment.getId(), e);
        }
    }

    /** @param created true if this call created a new deployment; false if it replayed an existing Idempotency-Key. */
    public record CreateResult(DeploymentResponse deployment, boolean created) {
    }

    /**
     * Idempotent create (master spec §12): a repeated {@code Idempotency-Key}
     * returns the same deployment instead of inserting a duplicate. The
     * uniqueness guarantee is the {@code idempotency_key} table's primary
     * key — a race between two concurrent requests with the same key is
     * resolved by catching the resulting constraint violation and re-reading
     * the row the other request committed, not by locking.
     */
    public Mono<CreateResult> createDeployment(CreateDeploymentRequest request, String idempotencyKey) {
        return findOrInsertDeployment(request, idempotencyKey)
                .flatMap(outcome -> {
                    if (!outcome.created()) {
                        return loadResponse(outcome.deploymentId()).map(resp -> new CreateResult(resp, false));
                    }
                    return attemptWorkflowStart(outcome.deploymentId(), request.siteId())
                            .then(loadResponse(outcome.deploymentId()))
                            .map(resp -> new CreateResult(resp, true));
                });
    }

    private record InsertOutcome(String deploymentId, boolean created) {
    }

    private Mono<InsertOutcome> findOrInsertDeployment(CreateDeploymentRequest request, String idempotencyKey) {
        return Mono.fromCallable(() -> idempotencyKeyRepository.findById(idempotencyKey))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(existing -> existing
                        .map(rec -> Mono.just(new InsertOutcome(rec.getDeploymentId(), false)))
                        .orElseGet(() -> insertNewDeployment(request, idempotencyKey)));
    }

    private Mono<InsertOutcome> insertNewDeployment(CreateDeploymentRequest request, String idempotencyKey) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
                    String id = IdGenerator.next("DEP");
                    Instant now = Instant.now();
                    Deployment deployment = new Deployment();
                    deployment.setId(id);
                    deployment.setSiteId(request.siteId());
                    deployment.setStatus(DeploymentStatus.REQUESTED.name());
                    deployment.setIdempotencyKey(idempotencyKey);
                    deployment.setRequestedBy(request.requestedBy());
                    deployment.setCreatedAt(now);
                    deployment.setUpdatedAt(now);
                    deploymentRepository.saveAndFlush(deployment);
                    idempotencyKeyRepository.saveAndFlush(new IdempotencyKeyRecord(idempotencyKey, id, now));
                    writeOutboxEvent(deployment, DeploymentEventType.REQUESTED, null);
                    return id;
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .map(id -> new InsertOutcome(id, true))
                .onErrorResume(DataIntegrityViolationException.class, ex -> Mono.fromCallable(() ->
                                idempotencyKeyRepository.findById(idempotencyKey)
                                        .map(rec -> new InsertOutcome(rec.getDeploymentId(), false))
                                        .orElseThrow(() -> ex))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * Asks Camunda to start {@code deployment-process} and records the
     * outcome as the deployment's {@code camunda-workflow-start} step
     * (docs/09-database-design.md — deployment_step). Shared by
     * {@link #createDeployment} and {@link #retryDeployment}.
     */
    private Mono<Void> attemptWorkflowStart(String deploymentId, String siteId) {
        return workflowService.startDeploymentProcess(deploymentId, siteId)
                .flatMap(result -> Mono.fromRunnable(() -> applyWorkflowResult(deploymentId, result))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    private void applyWorkflowResult(String deploymentId, StartResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            Deployment deployment = deploymentRepository.findById(deploymentId)
                    .orElseThrow(() -> NotFoundException.deployment(deploymentId));
            DeploymentStep step = stepRepository.findByDeploymentIdAndStepName(deploymentId, DeploymentStep.WORKFLOW_START)
                    .orElseGet(() -> {
                        DeploymentStep s = new DeploymentStep();
                        s.setId(IdGenerator.next("STEP"));
                        s.setDeploymentId(deploymentId);
                        s.setStepName(DeploymentStep.WORKFLOW_START);
                        s.setAttemptCount(0);
                        return s;
                    });
            Instant now = Instant.now();
            if (step.getStartedAt() == null) {
                step.setStartedAt(now);
            }

            // DISABLED leaves the deployment's status unchanged (no Camunda broker configured) —
            // no outbox event either, since nothing about the deployment actually changed.
            DeploymentEventType eventType = null;
            switch (result.outcome()) {
                case STARTED -> {
                    step.setAttemptCount(step.getAttemptCount() + 1);
                    step.setStatus(DeploymentStepStatus.COMPLETED.name());
                    step.setCompletedAt(now);
                    step.setLastError(null);
                    deployment.setWorkflowInstanceId(result.workflowInstanceId());
                    deployment.setStatus(DeploymentStatus.RUNNING.name());
                }
                case DISABLED -> {
                    step.setStatus(DeploymentStepStatus.PENDING.name());
                    log.info("Camunda disabled; deployment {} stays {}", deploymentId, deployment.getStatus());
                }
                case ERROR -> {
                    int attempts = step.getAttemptCount() + 1;
                    step.setAttemptCount(attempts);
                    step.setStatus(DeploymentStepStatus.FAILED.name());
                    step.setLastError(result.errorMessage());
                    deployment.setStatus(attempts >= MAX_ATTEMPTS_BEFORE_ESCALATION
                            ? DeploymentStatus.FAILED_REQUIRES_ATTENTION.name()
                            : DeploymentStatus.FAILED.name());
                    log.warn("Workflow start failed for deployment {} (attempt {}): {}", deploymentId, attempts, result.errorMessage());
                }
            }
            if (result.outcome() == StartResult.Outcome.STARTED) {
                eventType = DeploymentEventType.STARTED;
            } else if (result.outcome() == StartResult.Outcome.ERROR) {
                eventType = DeploymentEventType.FAILED;
            }

            deployment.setUpdatedAt(now);
            stepRepository.save(step);
            deploymentRepository.save(deployment);
            if (eventType != null) {
                writeOutboxEvent(deployment, eventType, step.getLastError());
            }
        });
    }

    public Mono<DeploymentResponse> getDeployment(String deploymentId) {
        return loadResponse(deploymentId);
    }

    public Mono<PageResponse<DeploymentResponse>> listDeployments(String status, String siteId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        return Mono.fromCallable(() -> {
                    Specification<Deployment> spec = buildSpecification(status, siteId);
                    Page<Deployment> result = deploymentRepository.findAll(spec, PageRequest.of(safePage, safeSize));
                    List<DeploymentResponse> content = result.getContent().stream()
                            .map(d -> toResponse(d, stepRepository.findByDeploymentId(d.getId())))
                            .toList();
                    return new PageResponse<>(content, safePage, safeSize, result.getTotalElements());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<Deployment> buildSpecification(String status, String siteId) {
        Specification<Deployment> spec = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (siteId != null && !siteId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("siteId"), siteId));
        }
        return spec;
    }

    /**
     * Re-attempts {@code camunda-workflow-start} for a deployment stuck in
     * FAILED/FAILED_REQUIRES_ATTENTION (docs/08-workflow.md). Provider job
     * workers that would retry an individual provisioning step are Phase 4;
     * this is the deployment-service's own retry of getting the workflow
     * running at all.
     */
    public Mono<DeploymentResponse> retryDeployment(String deploymentId) {
        return Mono.fromCallable(() -> {
                    Deployment deployment = deploymentRepository.findById(deploymentId)
                            .orElseThrow(() -> NotFoundException.deployment(deploymentId));
                    if (!DeploymentStatus.valueOf(deployment.getStatus()).isRetryable()) {
                        throw new ConflictException("Deployment " + deploymentId
                                + " is not in a retryable state (status=" + deployment.getStatus() + ")");
                    }
                    return deployment.getSiteId();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(siteId -> attemptWorkflowStart(deploymentId, siteId))
                .then(loadResponse(deploymentId));
    }

    /** Best-effort Zeebe cancellation (master spec §8) — failure to reach the broker never fails the API call. */
    public Mono<DeploymentResponse> cancelDeployment(String deploymentId) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
                    Deployment deployment = deploymentRepository.findById(deploymentId)
                            .orElseThrow(() -> NotFoundException.deployment(deploymentId));
                    if (DeploymentStatus.valueOf(deployment.getStatus()).isTerminal()) {
                        throw new ConflictException("Deployment " + deploymentId
                                + " is already in a terminal state (status=" + deployment.getStatus() + ")");
                    }
                    deployment.setStatus(DeploymentStatus.CANCELLED.name());
                    deployment.setUpdatedAt(Instant.now());
                    deploymentRepository.save(deployment);
                    writeOutboxEvent(deployment, DeploymentEventType.CANCELLED, null);
                    return deployment.getWorkflowInstanceId();
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(workflowService::cancelBestEffort)
                .then(loadResponse(deploymentId));
    }

    private Mono<DeploymentResponse> loadResponse(String deploymentId) {
        return Mono.fromCallable(() -> {
                    Deployment deployment = deploymentRepository.findById(deploymentId)
                            .orElseThrow(() -> NotFoundException.deployment(deploymentId));
                    return toResponse(deployment, stepRepository.findByDeploymentId(deploymentId));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private DeploymentResponse toResponse(Deployment deployment, List<DeploymentStep> steps) {
        List<DeploymentStepResponse> stepResponses = steps.stream()
                .map(s -> new DeploymentStepResponse(s.getId(), s.getStepName(), s.getStatus(), s.getAttemptCount(),
                        s.getLastError(), s.getStartedAt(), s.getCompletedAt()))
                .toList();
        return new DeploymentResponse(deployment.getId(), deployment.getSiteId(), deployment.getBatchId(),
                deployment.getStatus(), deployment.getWorkflowInstanceId(), deployment.getRequestedBy(),
                deployment.getCreatedAt(), deployment.getUpdatedAt(), stepResponses);
    }
}
