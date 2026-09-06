package com.ensap.siteprofile.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Guarantees every request/response carries {@code X-Correlation-Id}
 * (docs/10-api-design.md "Correlation") — generates one if the caller
 * didn't send it, so handlers reading the header never see null.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            request = request.mutate().header(HEADER, correlationId).build();
            exchange = exchange.mutate().request(request).build();
        }
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        return chain.filter(exchange);
    }
}
