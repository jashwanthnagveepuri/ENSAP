package com.ensap.deployment.service;

import com.ensap.deployment.dto.CreateDeploymentRequest;
import com.ensap.deployment.entity.Deployment;
import com.ensap.deployment.entity.DeploymentStatus;
import com.ensap.deployment.entity.DeploymentStep;
import com.ensap.deployment.entity.IdempotencyKeyRecord;
import com.ensap.deployment.exception.ConflictException;
import com.ensap.deployment.exception.NotFoundException;
import com.ensap.deployment.repository.DeploymentRepository;
import com.ensap.deployment.repository.DeploymentStepRepository;
import com.ensap.deployment.repository.IdempotencyKeyRepository;
import com.ensap.deployment.workflow.StartResult;
import com.ensap.deployment.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the create/retry/cancel state machine (master spec
 * §12/§37 Phase 2) with every collaborator mocked — no Spring context, no
 * DB. End-to-end HTTP+Postgres behavior is covered by
 * {@code DeploymentEndToEndIT}.
 */
@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock private DeploymentRepository deploymentRepository;
    @Mock private DeploymentStepRepository stepRepository;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock private WorkflowService workflowService;
    @Mock private PlatformTransactionManager transactionManager;

    private DeploymentService service;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        lenient().when(stepRepository.findByDeploymentId(anyString())).thenReturn(List.of());
        service = new DeploymentService(deploymentRepository, stepRepository, idempotencyKeyRepository,
                workflowService, transactionManager);
    }

    @Test
    void createDeployment_newKey_insertsAndStartsWorkflow() {
        when(idempotencyKeyRepository.findById("key-1")).thenReturn(Optional.empty());
        when(deploymentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyKeyRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(workflowService.startDeploymentProcess(anyString(), eq("SITE-1")))
                .thenReturn(Mono.just(StartResult.started("42")));
        when(stepRepository.findByDeploymentIdAndStepName(anyString(), eq(DeploymentStep.WORKFLOW_START)))
                .thenReturn(Optional.empty());
        when(deploymentRepository.findById(anyString())).thenAnswer(inv -> Optional.of(deploymentCapturedFrom(inv.getArgument(0))));

        CreateDeploymentRequest request = new CreateDeploymentRequest("SITE-1", "alice");

        StepVerifier.create(service.createDeployment(request, "key-1"))
                .assertNext(result -> {
                    assertThat(result.created()).isTrue();
                    assertThat(result.deployment().id()).startsWith("DEP-");
                    assertThat(result.deployment().status()).isEqualTo(DeploymentStatus.RUNNING.name());
                    assertThat(result.deployment().workflowInstanceId()).isEqualTo("42");
                })
                .verifyComplete();

        verify(deploymentRepository).saveAndFlush(any());
        verify(idempotencyKeyRepository).saveAndFlush(any());
    }

    @Test
    void createDeployment_knownKey_replaysWithoutInsertingOrStartingWorkflow() {
        Deployment existing = deployment("DEP-EXIST", DeploymentStatus.RUNNING);
        when(idempotencyKeyRepository.findById("key-2"))
                .thenReturn(Optional.of(new IdempotencyKeyRecord("key-2", "DEP-EXIST", Instant.now())));
        when(deploymentRepository.findById("DEP-EXIST")).thenReturn(Optional.of(existing));

        StepVerifier.create(service.createDeployment(new CreateDeploymentRequest("SITE-1", null), "key-2"))
                .assertNext(result -> {
                    assertThat(result.created()).isFalse();
                    assertThat(result.deployment().id()).isEqualTo("DEP-EXIST");
                })
                .verifyComplete();

        verify(deploymentRepository, never()).saveAndFlush(any());
        verify(workflowService, never()).startDeploymentProcess(anyString(), anyString());
    }

    @Test
    void createDeployment_raceOnInsert_returnsWinnerInsteadOfDuplicate() {
        Deployment winner = deployment("DEP-WINNER", DeploymentStatus.REQUESTED);
        when(idempotencyKeyRepository.findById("key-3"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new IdempotencyKeyRecord("key-3", "DEP-WINNER", Instant.now())));
        when(deploymentRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(idempotencyKeyRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(deploymentRepository.findById("DEP-WINNER")).thenReturn(Optional.of(winner));

        StepVerifier.create(service.createDeployment(new CreateDeploymentRequest("SITE-1", null), "key-3"))
                .assertNext(result -> {
                    assertThat(result.created()).isFalse();
                    assertThat(result.deployment().id()).isEqualTo("DEP-WINNER");
                })
                .verifyComplete();

        verify(workflowService, never()).startDeploymentProcess(anyString(), anyString());
    }

    @Test
    void retryDeployment_fromFailed_reAttemptsWorkflowStart() {
        Deployment failed = deployment("DEP-1", DeploymentStatus.FAILED);
        when(deploymentRepository.findById("DEP-1")).thenReturn(Optional.of(failed));
        when(stepRepository.findByDeploymentIdAndStepName("DEP-1", DeploymentStep.WORKFLOW_START)).thenReturn(Optional.empty());
        when(workflowService.startDeploymentProcess("DEP-1", "SITE-1")).thenReturn(Mono.just(StartResult.started("99")));

        StepVerifier.create(service.retryDeployment("DEP-1"))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo(DeploymentStatus.RUNNING.name());
                    assertThat(response.workflowInstanceId()).isEqualTo("99");
                })
                .verifyComplete();
    }

    @Test
    void retryDeployment_whenNotFailed_conflicts() {
        when(deploymentRepository.findById("DEP-2")).thenReturn(Optional.of(deployment("DEP-2", DeploymentStatus.RUNNING)));

        StepVerifier.create(service.retryDeployment("DEP-2"))
                .expectError(ConflictException.class)
                .verify();

        verify(workflowService, never()).startDeploymentProcess(anyString(), anyString());
    }

    @Test
    void retryDeployment_thirdFailure_escalatesToRequiresAttention() {
        Deployment failing = deployment("DEP-3", DeploymentStatus.FAILED);
        DeploymentStep step = new DeploymentStep();
        step.setId("STEP-1");
        step.setDeploymentId("DEP-3");
        step.setStepName(DeploymentStep.WORKFLOW_START);
        step.setAttemptCount(2);
        when(deploymentRepository.findById("DEP-3")).thenReturn(Optional.of(failing));
        when(stepRepository.findByDeploymentIdAndStepName("DEP-3", DeploymentStep.WORKFLOW_START)).thenReturn(Optional.of(step));
        when(workflowService.startDeploymentProcess("DEP-3", "SITE-1")).thenReturn(Mono.just(StartResult.error("broker down")));

        StepVerifier.create(service.retryDeployment("DEP-3"))
                .assertNext(response -> assertThat(response.status()).isEqualTo(DeploymentStatus.FAILED_REQUIRES_ATTENTION.name()))
                .verifyComplete();
    }

    @Test
    void cancelDeployment_fromRunning_cancelsAndBestEffortNotifiesWorkflow() {
        Deployment running = deployment("DEP-4", DeploymentStatus.RUNNING);
        running.setWorkflowInstanceId("77");
        when(deploymentRepository.findById("DEP-4")).thenReturn(Optional.of(running));
        when(workflowService.cancelBestEffort("77")).thenReturn(Mono.empty());

        StepVerifier.create(service.cancelDeployment("DEP-4"))
                .assertNext(response -> assertThat(response.status()).isEqualTo(DeploymentStatus.CANCELLED.name()))
                .verifyComplete();

        verify(workflowService).cancelBestEffort("77");
    }

    @Test
    void cancelDeployment_whenAlreadyTerminal_conflicts() {
        when(deploymentRepository.findById("DEP-5")).thenReturn(Optional.of(deployment("DEP-5", DeploymentStatus.COMPLETED)));

        StepVerifier.create(service.cancelDeployment("DEP-5"))
                .expectError(ConflictException.class)
                .verify();

        verify(workflowService, never()).cancelBestEffort(any());
    }

    @Test
    void getDeployment_notFound_errors() {
        when(deploymentRepository.findById("DEP-MISSING")).thenReturn(Optional.empty());

        StepVerifier.create(service.getDeployment("DEP-MISSING"))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void listDeployments_appliesPagingAndMapsContent() {
        Page<Deployment> page = new PageImpl<>(List.of(deployment("DEP-6", DeploymentStatus.REQUESTED)));
        when(deploymentRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        StepVerifier.create(service.listDeployments("REQUESTED", null, 0, 20))
                .assertNext(result -> {
                    assertThat(result.content()).hasSize(1);
                    assertThat(result.content().get(0).id()).isEqualTo("DEP-6");
                    assertThat(result.totalElements()).isEqualTo(1);
                })
                .verifyComplete();
    }

    private Deployment deployment(String id, DeploymentStatus status) {
        Deployment deployment = new Deployment();
        deployment.setId(id);
        deployment.setSiteId("SITE-1");
        deployment.setStatus(status.name());
        deployment.setCreatedAt(Instant.now());
        deployment.setUpdatedAt(Instant.now());
        return deployment;
    }

    private Deployment deploymentCapturedFrom(String id) {
        Deployment deployment = new Deployment();
        deployment.setId(id);
        deployment.setSiteId("SITE-1");
        deployment.setStatus(DeploymentStatus.RUNNING.name());
        deployment.setWorkflowInstanceId("42");
        deployment.setCreatedAt(Instant.now());
        deployment.setUpdatedAt(Instant.now());
        return deployment;
    }
}
