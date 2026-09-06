package com.ensap.deployment.router;

import com.ensap.deployment.handler.DeploymentHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * HTTP routing only for deployment endpoints (docs/10-api-design.md) — no
 * business logic here (master spec §32). Composed into
 * {@link com.ensap.deployment.config.RouterConfig}.
 */
@Configuration
public class DeploymentRouter {

    @Bean
    public RouterFunction<ServerResponse> deploymentRoutes(DeploymentHandler deploymentHandler) {
        return route()
                .GET("/api/deployments", deploymentHandler::listDeployments)
                .POST("/api/deployments", deploymentHandler::createDeployment)
                .GET("/api/deployments/{deploymentId}", deploymentHandler::getDeployment)
                .POST("/api/deployments/{deploymentId}/retry", deploymentHandler::retryDeployment)
                .POST("/api/deployments/{deploymentId}/cancel", deploymentHandler::cancelDeployment)
                .build();
    }
}
