package com.ensap.observability;

import io.micrometer.context.ContextRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Hooks;

/**
 * Auto-registers {@link CorrelationIdWebFilter} and wires the correlation MDC accessors into
 * Reactor's context propagation. Picked up via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} —
 * a service just needs this jar on the classpath, no manual {@code @Import} required.
 */
@AutoConfiguration
@ConditionalOnClass(WebFilter.class)
public class ObservabilityAutoConfiguration {

    public ObservabilityAutoConfiguration() {
        ContextRegistry registry = ContextRegistry.getInstance();
        for (String key : CorrelationConstants.PROPAGATED_KEYS) {
            // Defensive re-registration: safe if this constructor runs more than once
            // (e.g. multiple ApplicationContexts in a test suite).
            registry.removeThreadLocalAccessor(key);
            registry.registerThreadLocalAccessor(new MdcContextAccessor(key));
        }
        Hooks.enableAutomaticContextPropagation();
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdWebFilter correlationIdWebFilter() {
        return new CorrelationIdWebFilter();
    }
}
