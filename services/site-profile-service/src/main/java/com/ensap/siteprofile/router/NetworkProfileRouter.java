package com.ensap.siteprofile.router;

import com.ensap.siteprofile.handler.NetworkProfileHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** HTTP routing for network-profile endpoints nested under a site (docs/10-api-design.md). */
@Configuration
public class NetworkProfileRouter {

    @Bean
    public RouterFunction<ServerResponse> networkProfileRoutes(NetworkProfileHandler networkProfileHandler) {
        return route()
                .POST("/api/sites/{siteId}/network-profiles", networkProfileHandler::createProfile)
                .GET("/api/sites/{siteId}/network-profiles", networkProfileHandler::listProfiles)
                .GET("/api/sites/{siteId}/network-profiles/{profileId}", networkProfileHandler::getProfile)
                .PUT("/api/sites/{siteId}/network-profiles/{profileId}", networkProfileHandler::updateProfile)
                .DELETE("/api/sites/{siteId}/network-profiles/{profileId}", networkProfileHandler::deleteProfile)
                .build();
    }
}
