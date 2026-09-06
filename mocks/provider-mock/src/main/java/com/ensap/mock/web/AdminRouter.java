package com.ensap.mock.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** HTTP routing for the Provider-Simulation admin API (master spec §22). */
@Configuration
public class AdminRouter {

    @Bean
    public RouterFunction<ServerResponse> adminRoutes(AdminHandler adminHandler) {
        return route()
                .GET("/admin/providers", adminHandler::listAll)
                .GET("/admin/providers/{providerType}", adminHandler::getOne)
                .PUT("/admin/providers/{providerType}", adminHandler::update)
                .POST("/admin/providers/{providerType}/reset", adminHandler::reset)
                .build();
    }
}
