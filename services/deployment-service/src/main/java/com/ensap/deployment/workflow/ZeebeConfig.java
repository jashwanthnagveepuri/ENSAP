package com.ensap.deployment.workflow;

import io.camunda.zeebe.client.ZeebeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Zeebe client for Camunda 8 workflow orchestration (master spec
 * §8). No bean at all when {@code deployment.camunda.enabled=false} (tests
 * that don't run a Zeebe broker) — {@link WorkflowService} treats a missing
 * client the same as disabled.
 */
@Configuration
public class ZeebeConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "deployment.camunda", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ZeebeClient zeebeClient(@Value("${deployment.camunda.gateway-address:localhost:26500}") String gatewayAddress) {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(gatewayAddress)
                .usePlaintext()
                .build();
    }
}
