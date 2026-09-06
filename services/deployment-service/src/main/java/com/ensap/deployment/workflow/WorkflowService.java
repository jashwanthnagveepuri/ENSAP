package com.ensap.deployment.workflow;

import io.camunda.zeebe.client.ZeebeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;

/**
 * Starts and cancels instances of {@code deployment-process}
 * (workflow/camunda/deployment-process.bpmn, master spec §8). The Zeebe
 * client call is blocking (it's a synchronous gRPC round-trip under
 * {@code .join()}), so it runs on {@code boundedElastic} like the JPA calls
 * elsewhere in this service.
 */
@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);
    public static final String PROCESS_ID = "deployment-process";

    private final ObjectProvider<ZeebeClient> zeebeClientProvider;
    private final boolean enabled;
    private final Duration callTimeout;

    public WorkflowService(ObjectProvider<ZeebeClient> zeebeClientProvider,
                            @Value("${deployment.camunda.enabled:true}") boolean enabled,
                            @Value("${deployment.camunda.call-timeout-seconds:10}") long callTimeoutSeconds) {
        this.zeebeClientProvider = zeebeClientProvider;
        this.enabled = enabled;
        this.callTimeout = Duration.ofSeconds(callTimeoutSeconds);
    }

    public Mono<StartResult> startDeploymentProcess(String deploymentId, String siteId) {
        ZeebeClient client = enabled ? zeebeClientProvider.getIfAvailable() : null;
        if (client == null) {
            return Mono.just(StartResult.disabled());
        }
        return Mono.fromCallable(() -> client.newCreateInstanceCommand()
                        .bpmnProcessId(PROCESS_ID)
                        .latestVersion()
                        .variables(Map.of("deploymentId", deploymentId, "siteId", siteId))
                        .send()
                        .join())
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(callTimeout)
                .map(result -> StartResult.started(String.valueOf(result.getProcessInstanceKey())))
                .onErrorResume(ex -> {
                    log.warn("Failed to start {} for deployment {}: {}", PROCESS_ID, deploymentId, ex.toString());
                    return Mono.just(StartResult.error(ex.getMessage() != null ? ex.getMessage() : ex.toString()));
                });
    }

    /** Best-effort: cancellation failing (broker down, instance already gone) never fails the API call. */
    public Mono<Void> cancelBestEffort(String workflowInstanceId) {
        ZeebeClient client = enabled ? zeebeClientProvider.getIfAvailable() : null;
        if (client == null || workflowInstanceId == null) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> client.newCancelInstanceCommand(Long.parseLong(workflowInstanceId)).send().join())
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(callTimeout)
                .onErrorResume(ex -> {
                    log.warn("Failed to cancel process instance {}: {}", workflowInstanceId, ex.toString());
                    return Mono.empty();
                })
                .then();
    }
}
