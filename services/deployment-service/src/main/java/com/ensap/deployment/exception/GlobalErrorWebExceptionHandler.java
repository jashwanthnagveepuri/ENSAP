package com.ensap.deployment.exception;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Defense-in-depth fallback so anything a handler's own error handling
 * doesn't catch (a filter throwing, a malformed-body codec failure before
 * routing) still comes back as the shared {@link ApiError} JSON shape
 * (docs/10-api-design.md §31) instead of Spring's default HTML error page.
 * Handlers should still map their own known exceptions via {@link ErrorMapper}
 * for the right status code — this only guarantees the *shape*.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
                                           ApplicationContext applicationContext, ServerCodecConfigurer codecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        setMessageWriters(codecConfigurer.getWriters());
        setMessageReaders(codecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(req -> true, this::renderError);
    }

    private Mono<ServerResponse> renderError(ServerRequest request) {
        var attrs = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        int status = (int) attrs.getOrDefault("status", 500);
        String correlationId = request.headers().firstHeader("X-Correlation-Id");
        ApiError body = ApiError.of(status, "INTERNAL_ERROR", String.valueOf(attrs.get("error")), correlationId);
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body));
    }
}
