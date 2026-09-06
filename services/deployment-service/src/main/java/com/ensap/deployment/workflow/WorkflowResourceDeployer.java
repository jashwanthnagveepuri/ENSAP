package com.ensap.deployment.workflow;

import io.camunda.zeebe.client.ZeebeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Deploys {@code deployment-process.bpmn} to Zeebe on startup (master spec
 * §8). Best-effort: if the broker isn't reachable yet (or Camunda is
 * disabled for this run), this logs a warning and the app still starts —
 * {@link WorkflowService} then reports {@code ERROR}/{@code DISABLED} per
 * deployment instead of the whole service failing to boot.
 */
@Component
public class WorkflowResourceDeployer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowResourceDeployer.class);
    private static final String RESOURCE_PATH = "camunda/deployment-process.bpmn";

    private final ObjectProvider<ZeebeClient> zeebeClientProvider;
    private final boolean enabled;

    public WorkflowResourceDeployer(ObjectProvider<ZeebeClient> zeebeClientProvider,
                                     @Value("${deployment.camunda.enabled:true}") boolean enabled) {
        this.zeebeClientProvider = zeebeClientProvider;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("deployment.camunda.enabled=false; skipping {} deploy", RESOURCE_PATH);
            return;
        }
        ZeebeClient client = zeebeClientProvider.getIfAvailable();
        if (client == null) {
            log.warn("No ZeebeClient bean available; skipping {} deploy", RESOURCE_PATH);
            return;
        }
        try {
            client.newDeployResourceCommand().addResourceFromClasspath(RESOURCE_PATH).send().join();
            log.info("Deployed {} to Zeebe", RESOURCE_PATH);
        } catch (Exception ex) {
            log.warn("Could not deploy {} to Zeebe (broker unreachable?): {}", RESOURCE_PATH, ex.toString());
        }
    }
}
