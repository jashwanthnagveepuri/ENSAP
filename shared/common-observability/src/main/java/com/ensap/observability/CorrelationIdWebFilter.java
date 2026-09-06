package com.ensap.observability;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * Reads (or generates) the correlation-id family of headers on every request, echoes
 * {@code correlationId} back on the response, and writes all present values into the Reactor
 * {@link Context} so they propagate through the whole reactive chain and into logs via
 * {@link MdcContextAccessor} (see {@link ObservabilityAutoConfiguration}).
 */
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String correlationId = firstNonBlank(
                request.getHeaders().getFirst(CorrelationConstants.HEADER_CORRELATION_ID),
                UUID.randomUUID().toString());
        String siteId = request.getHeaders().getFirst(CorrelationConstants.HEADER_SITE_ID);
        String deploymentId = request.getHeaders().getFirst(CorrelationConstants.HEADER_DEPLOYMENT_ID);
        String workflowInstanceId = request.getHeaders().getFirst(CorrelationConstants.HEADER_WORKFLOW_INSTANCE_ID);
        String operationId = request.getHeaders().getFirst(CorrelationConstants.HEADER_OPERATION_ID);

        exchange.getResponse().getHeaders().set(CorrelationConstants.HEADER_CORRELATION_ID, correlationId);

        return chain.filter(exchange)
                .contextWrite(ctx -> putIfPresent(ctx, CorrelationConstants.SITE_ID, siteId))
                .contextWrite(ctx -> putIfPresent(ctx, CorrelationConstants.DEPLOYMENT_ID, deploymentId))
                .contextWrite(ctx -> putIfPresent(ctx, CorrelationConstants.WORKFLOW_INSTANCE_ID, workflowInstanceId))
                .contextWrite(ctx -> putIfPresent(ctx, CorrelationConstants.OPERATION_ID, operationId))
                .contextWrite(Context.of(CorrelationConstants.CORRELATION_ID, correlationId));
    }

    private static Context putIfPresent(Context ctx, String key, String value) {
        return value == null || value.isBlank() ? ctx : ctx.put(key, value);
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
