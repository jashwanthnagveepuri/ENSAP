package com.ensap.siteprofile.cache;

import com.ensap.siteprofile.dto.SiteDetailResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis read-through cache for {@code GET /api/sites/{siteId}} (master spec
 * §14: "site profile caching"). Every operation fails open — on any Redis
 * error the caller falls back to PostgreSQL, since §14 requires the system
 * stay "recoverable" with Redis down; the cache is a speed-up, never a
 * dependency.
 */
@Component
public class SiteCacheService {

    private static final Logger log = LoggerFactory.getLogger(SiteCacheService.class);
    private static final Duration TTL = Duration.ofSeconds(60);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SiteCacheService(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<SiteDetailResponse> get(String siteId) {
        return redisTemplate.opsForValue().get(key(siteId))
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, SiteDetailResponse.class));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .onErrorResume(ex -> {
                    log.warn("Site cache read skipped (Redis unavailable): {}", ex.toString());
                    return Mono.empty();
                });
    }

    public Mono<Void> put(String siteId, SiteDetailResponse detail) {
        try {
            String json = objectMapper.writeValueAsString(detail);
            return redisTemplate.opsForValue().set(key(siteId), json, TTL)
                    .onErrorResume(ex -> {
                        log.warn("Site cache write skipped (Redis unavailable): {}", ex.toString());
                        return Mono.just(true);
                    })
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }

    public Mono<Void> evict(String siteId) {
        return redisTemplate.delete(key(siteId))
                .onErrorResume(ex -> {
                    log.warn("Site cache evict skipped (Redis unavailable): {}", ex.toString());
                    return Mono.just(0L);
                })
                .then();
    }

    private String key(String siteId) {
        return "site-profile:" + siteId;
    }
}
