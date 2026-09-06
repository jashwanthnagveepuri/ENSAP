package com.ensap.evidenceaudit.router;

import com.ensap.evidenceaudit.handler.AuditHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * HTTP routing only for audit endpoints (docs/10-api-design.md) — no
 * business logic here (master spec §32). Composed into
 * {@link com.ensap.evidenceaudit.config.RouterConfig}.
 */
@Configuration
public class AuditRouter {

    @Bean
    public RouterFunction<ServerResponse> auditRoutes(AuditHandler auditHandler) {
        return route()
                .GET("/api/audit-events", auditHandler::listAuditEvents)
                .build();
    }
}
