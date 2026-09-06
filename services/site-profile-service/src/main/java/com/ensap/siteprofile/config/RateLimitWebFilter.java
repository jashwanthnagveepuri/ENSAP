package com.ensap.siteprofile.config;

import com.ensap.siteprofile.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Simple fixed-window API rate limit backed by Redis (master spec §14).
 * One INCR/EXPIRE per client per minute per path prefix; over the limit
 * returns 429. Any Redis error (including Redis being down) fails OPEN —
 * the request proceeds — since Redis must never be a hard dependency for
 * availability (§14: "the system must remain recoverable" without it).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitWebFilter.class);
    private static final int LIMIT_PER_MINUTE = 120;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitWebFilter(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!request.getPath().value().startsWith("/api/")) {
            return chain.filter(exchange);
        }

        String clientId = Objects.requireNonNullElse(
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : null,
                "unknown");
        String key = "ratelimit:" + clientId + ":" + (Instant.now().getEpochSecond() / 60);

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> ensureExpiry = count == 1
                            ? redisTemplate.expire(key, Duration.ofSeconds(60))
                            : Mono.just(true);
                    return ensureExpiry.then(Mono.just(count));
                })
                .onErrorResume(ex -> {
                    log.warn("Rate limit check skipped (Redis unavailable): {}", ex.toString());
                    return Mono.just(0L);
                })
                .flatMap(count -> {
                    if (count > LIMIT_PER_MINUTE) {
                        return tooManyRequests(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationIdWebFilter.HEADER);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError body = ApiError.of(429, "RATE_LIMIT_EXCEEDED", "Too many requests, slow down.", correlationId);
        byte[] json;
        try {
            json = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            json = "{}".getBytes(StandardCharsets.UTF_8);
        }
        var buffer = exchange.getResponse().bufferFactory().wrap(json);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
