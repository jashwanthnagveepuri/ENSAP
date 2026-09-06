package com.ensap.observability;

import io.micrometer.context.ContextRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @BeforeAll
    static void enablePropagation() {
        // Mirrors what ObservabilityAutoConfiguration does in a real service.
        for (String key : CorrelationConstants.PROPAGATED_KEYS) {
            ContextRegistry.getInstance().registerThreadLocalAccessor(new MdcContextAccessor(key));
        }
        Hooks.enableAutomaticContextPropagation();
    }

    @AfterAll
    static void resetPropagation() {
        Hooks.disableAutomaticContextPropagation();
    }

    @Test
    void generatesCorrelationIdWhenAbsentAndEchoesItOnTheResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/sites"));

        filter.filter(exchange, ex -> Mono.empty()).block();

        String responseHeader = exchange.getResponse().getHeaders()
                .getFirst(CorrelationConstants.HEADER_CORRELATION_ID);
        assertThat(responseHeader).isNotBlank();
    }

    @Test
    void echoesAnIncomingCorrelationIdInsteadOfGeneratingANewOne() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/sites")
                        .header(CorrelationConstants.HEADER_CORRELATION_ID, "given-correlation-id"));

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationConstants.HEADER_CORRELATION_ID))
                .isEqualTo("given-correlation-id");
    }

    @Test
    void propagatesCorrelationFieldsIntoMdcForDownstreamLogging() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/deployments/123")
                        .header(CorrelationConstants.HEADER_CORRELATION_ID, "corr-1")
                        .header(CorrelationConstants.HEADER_SITE_ID, "site-42")
                        .header(CorrelationConstants.HEADER_DEPLOYMENT_ID, "dep-99"));

        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();
        AtomicReference<String> mdcSiteId = new AtomicReference<>();
        AtomicReference<String> mdcDeploymentId = new AtomicReference<>();
        AtomicReference<String> mdcOperationId = new AtomicReference<>();

        // Simulates a downstream handler that logs mid-chain: automatic context propagation
        // restores MDC from the Reactor Context around this operator even without a thread switch.
        WebFilterChainStub chain = new WebFilterChainStub(Mono.fromRunnable(() -> {
            mdcCorrelationId.set(MDC.get(CorrelationConstants.CORRELATION_ID));
            mdcSiteId.set(MDC.get(CorrelationConstants.SITE_ID));
            mdcDeploymentId.set(MDC.get(CorrelationConstants.DEPLOYMENT_ID));
            mdcOperationId.set(MDC.get(CorrelationConstants.OPERATION_ID));
        }));

        filter.filter(exchange, chain).block();

        assertThat(mdcCorrelationId.get()).isEqualTo("corr-1");
        assertThat(mdcSiteId.get()).isEqualTo("site-42");
        assertThat(mdcDeploymentId.get()).isEqualTo("dep-99");
        assertThat(mdcOperationId.get()).isNull();
    }

    /** Minimal {@code WebFilterChain} stub — avoids pulling in a mocking library for one method. */
    private record WebFilterChainStub(Mono<Void> result) implements org.springframework.web.server.WebFilterChain {
        @Override
        public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange) {
            return result;
        }
    }
}
