package com.ensap.mock.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** HTTP routing for the five provider mocks — path-based, one app (master spec §22). */
@Configuration
public class ProviderRouter {

    @Bean
    public RouterFunction<ServerResponse> providerRoutes(ProviderHandler providerHandler) {
        return route()
                .POST("/api/v1/{providerType}/jobs", providerHandler::createJob)
                .build();
    }
}
