package com.ensap.siteprofile.handler;

import com.ensap.siteprofile.dto.DeviceRequest;
import com.ensap.siteprofile.exception.ErrorMapper;
import com.ensap.siteprofile.exception.RequestValidator;
import com.ensap.siteprofile.service.DeviceService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** Translates HTTP requests/responses for device endpoints nested under a site. */
@Component
public class DeviceHandler {

    private final DeviceService deviceService;
    private final RequestValidator validator;

    public DeviceHandler(DeviceService deviceService, RequestValidator validator) {
        this.deviceService = deviceService;
        this.validator = validator;
    }

    public Mono<ServerResponse> listDevices(ServerRequest request) {
        String correlationId = correlationId(request);
        return deviceService.listDevices(request.pathVariable("siteId"))
                .flatMap(devices -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(devices))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> getDevice(ServerRequest request) {
        String correlationId = correlationId(request);
        return deviceService.getDevice(request.pathVariable("siteId"), request.pathVariable("deviceId"))
                .flatMap(device -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(device))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> createDevice(ServerRequest request) {
        String correlationId = correlationId(request);
        String siteId = request.pathVariable("siteId");
        return request.bodyToMono(DeviceRequest.class)
                .map(validator::validate)
                .flatMap(body -> deviceService.createDevice(siteId, body))
                .flatMap(device -> ServerResponse.status(201).contentType(MediaType.APPLICATION_JSON).bodyValue(device))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> updateDevice(ServerRequest request) {
        String correlationId = correlationId(request);
        String siteId = request.pathVariable("siteId");
        String deviceId = request.pathVariable("deviceId");
        return request.bodyToMono(DeviceRequest.class)
                .map(validator::validate)
                .flatMap(body -> deviceService.updateDevice(siteId, deviceId, body))
                .flatMap(device -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(device))
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    public Mono<ServerResponse> deleteDevice(ServerRequest request) {
        String correlationId = correlationId(request);
        return deviceService.deleteDevice(request.pathVariable("siteId"), request.pathVariable("deviceId"))
                .then(ServerResponse.noContent().build())
                .onErrorResume(ex -> ErrorMapper.toResponse(ex, correlationId));
    }

    private String correlationId(ServerRequest request) {
        return request.headers().firstHeader("X-Correlation-Id");
    }
}
