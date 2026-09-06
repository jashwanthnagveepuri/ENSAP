package com.ensap.siteprofile.router;

import com.ensap.siteprofile.handler.DeviceHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/** HTTP routing for device endpoints nested under a site (docs/10-api-design.md). */
@Configuration
public class DeviceRouter {

    @Bean
    public RouterFunction<ServerResponse> deviceRoutes(DeviceHandler deviceHandler) {
        return route()
                .POST("/api/sites/{siteId}/devices", deviceHandler::createDevice)
                .GET("/api/sites/{siteId}/devices", deviceHandler::listDevices)
                .GET("/api/sites/{siteId}/devices/{deviceId}", deviceHandler::getDevice)
                .PUT("/api/sites/{siteId}/devices/{deviceId}", deviceHandler::updateDevice)
                .DELETE("/api/sites/{siteId}/devices/{deviceId}", deviceHandler::deleteDevice)
                .build();
    }
}
