package com.ensap.siteprofile.service;

import com.ensap.siteprofile.cache.SiteCacheService;
import com.ensap.siteprofile.dto.DeviceRequest;
import com.ensap.siteprofile.dto.DeviceResponse;
import com.ensap.siteprofile.entity.Device;
import com.ensap.siteprofile.exception.NotFoundException;
import com.ensap.siteprofile.repository.DeviceRepository;
import com.ensap.siteprofile.repository.SiteRepository;
import com.ensap.siteprofile.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;

/**
 * CRUD for devices owned by a site (docs/06-component-design.md). A site's
 * cached detail ({@link SiteCacheService}) is evicted on any write so
 * {@code GET /api/sites/{siteId}} doesn't serve stale device lists.
 */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final SiteCacheService cacheService;

    public DeviceService(DeviceRepository deviceRepository, SiteRepository siteRepository, SiteCacheService cacheService) {
        this.deviceRepository = deviceRepository;
        this.siteRepository = siteRepository;
        this.cacheService = cacheService;
    }

    public Mono<List<DeviceResponse>> listDevices(String siteId) {
        return requireSite(siteId)
                .then(Mono.fromCallable(() -> deviceRepository.findBySiteId(siteId)
                                .stream().map(SiteMapper::toResponse).toList())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<DeviceResponse> getDevice(String siteId, String deviceId) {
        return findOwned(siteId, deviceId).map(SiteMapper::toResponse);
    }

    public Mono<DeviceResponse> createDevice(String siteId, DeviceRequest request) {
        return requireSite(siteId)
                .then(Mono.fromCallable(() -> {
                    Device device = new Device();
                    device.setId(IdGenerator.next("DEV"));
                    device.setSiteId(siteId);
                    device.setType(request.type());
                    device.setVendor(request.vendor());
                    device.setModel(request.model());
                    device.setSerialNumber(request.serialNumber());
                    device.setStatus(request.status());
                    device.setCreatedAt(Instant.now());
                    return deviceRepository.save(device);
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(device -> cacheService.evict(siteId).thenReturn(device))
                .map(SiteMapper::toResponse);
    }

    public Mono<DeviceResponse> updateDevice(String siteId, String deviceId, DeviceRequest request) {
        return findOwned(siteId, deviceId)
                .flatMap(device -> Mono.fromCallable(() -> {
                    device.setType(request.type());
                    device.setVendor(request.vendor());
                    device.setModel(request.model());
                    device.setSerialNumber(request.serialNumber());
                    device.setStatus(request.status());
                    return deviceRepository.save(device);
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(device -> cacheService.evict(siteId).thenReturn(device))
                .map(SiteMapper::toResponse);
    }

    public Mono<Void> deleteDevice(String siteId, String deviceId) {
        return findOwned(siteId, deviceId)
                .flatMap(device -> Mono.fromRunnable(() -> deviceRepository.deleteById(deviceId))
                        .subscribeOn(Schedulers.boundedElastic()).then())
                .then(cacheService.evict(siteId));
    }

    private Mono<Void> requireSite(String siteId) {
        return Mono.fromCallable(() -> siteRepository.existsById(siteId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> exists ? Mono.<Void>empty() : Mono.error(NotFoundException.site(siteId)));
    }

    private Mono<Device> findOwned(String siteId, String deviceId) {
        return Mono.fromCallable(() -> deviceRepository.findById(deviceId)
                        .filter(d -> d.getSiteId().equals(siteId))
                        .orElseThrow(() -> NotFoundException.device(siteId, deviceId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
