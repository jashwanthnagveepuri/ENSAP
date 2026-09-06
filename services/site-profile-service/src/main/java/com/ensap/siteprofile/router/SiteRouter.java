package com.ensap.siteprofile.router;

import com.ensap.siteprofile.handler.SiteHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * HTTP routing only for site endpoints (docs/10-api-design.md) — no business
 * logic here (master spec §32). Composed into {@link com.ensap.siteprofile.config.RouterConfig}.
 */
@Configuration
public class SiteRouter {

    @Bean
    public RouterFunction<ServerResponse> siteRoutes(SiteHandler siteHandler) {
        return route()
                .POST("/api/sites", siteHandler::createSite)
                .GET("/api/sites", siteHandler::listSites)
                .GET("/api/sites/{siteId}", siteHandler::getSite)
                .PUT("/api/sites/{siteId}", siteHandler::updateSite)
                .DELETE("/api/sites/{siteId}", siteHandler::deleteSite)
                .POST("/api/sites/{siteId}/refresh", siteHandler::refreshSite)
                .build();
    }
}
