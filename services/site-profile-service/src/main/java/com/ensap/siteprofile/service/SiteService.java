package com.ensap.siteprofile.service;

import com.ensap.siteprofile.cache.SiteCacheService;
import com.ensap.siteprofile.client.InventoryServiceClient;
import com.ensap.siteprofile.client.LocationServiceClient;
import com.ensap.siteprofile.client.NetworkServiceClient;
import com.ensap.siteprofile.dto.PageResponse;
import com.ensap.siteprofile.dto.SiteDetailResponse;
import com.ensap.siteprofile.dto.SiteRequest;
import com.ensap.siteprofile.dto.SiteResponse;
import com.ensap.siteprofile.entity.Device;
import com.ensap.siteprofile.entity.NetworkProfile;
import com.ensap.siteprofile.entity.Site;
import com.ensap.siteprofile.entity.Subnet;
import com.ensap.siteprofile.entity.Vlan;
import com.ensap.siteprofile.entity.WanCircuit;
import com.ensap.siteprofile.exception.NotFoundException;
import com.ensap.siteprofile.repository.DeviceRepository;
import com.ensap.siteprofile.repository.NetworkProfileRepository;
import com.ensap.siteprofile.repository.SiteRepository;
import com.ensap.siteprofile.repository.SubnetRepository;
import com.ensap.siteprofile.repository.VlanRepository;
import com.ensap.siteprofile.repository.WanCircuitRepository;
import com.ensap.siteprofile.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for site profiles (docs/06-component-design.md — Service
 * layer; docs/02-functional-requirements.md FR-1). Repository calls are
 * blocking JPA (Phase 0 chose spring-data-jpa, not R2DBC) so every DB
 * access is wrapped in {@code Mono.fromCallable(...).subscribeOn(boundedElastic())}
 * to keep the WebFlux event loop free.
 */
@Service
public class SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository siteRepository;
    private final DeviceRepository deviceRepository;
    private final NetworkProfileRepository networkProfileRepository;
    private final VlanRepository vlanRepository;
    private final SubnetRepository subnetRepository;
    private final WanCircuitRepository wanCircuitRepository;
    private final SiteCacheService cacheService;
    private final LocationServiceClient locationServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final NetworkServiceClient networkServiceClient;

    public SiteService(SiteRepository siteRepository, DeviceRepository deviceRepository,
                        NetworkProfileRepository networkProfileRepository, VlanRepository vlanRepository,
                        SubnetRepository subnetRepository, WanCircuitRepository wanCircuitRepository,
                        SiteCacheService cacheService, LocationServiceClient locationServiceClient,
                        InventoryServiceClient inventoryServiceClient, NetworkServiceClient networkServiceClient) {
        this.siteRepository = siteRepository;
        this.deviceRepository = deviceRepository;
        this.networkProfileRepository = networkProfileRepository;
        this.vlanRepository = vlanRepository;
        this.subnetRepository = subnetRepository;
        this.wanCircuitRepository = wanCircuitRepository;
        this.cacheService = cacheService;
        this.locationServiceClient = locationServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
        this.networkServiceClient = networkServiceClient;
    }

    public Mono<SiteResponse> createSite(SiteRequest request) {
        return Mono.fromCallable(() -> {
            String id = (request.id() == null || request.id().isBlank()) ? IdGenerator.next("SITE") : request.id();
            if (siteRepository.existsById(id)) {
                throw new IllegalArgumentException("Site " + id + " already exists");
            }
            Instant now = Instant.now();
            Site site = new Site();
            site.setId(id);
            site.setName(request.name());
            site.setRegion(request.region());
            site.setStatus(request.status());
            site.setSourceSystem("manual");
            site.setCreatedAt(now);
            site.setUpdatedAt(now);
            Site saved = siteRepository.save(site);
            saveWanCircuits(saved.getId(), request.wanCircuits());
            return saved;
        }).subscribeOn(Schedulers.boundedElastic()).map(SiteMapper::toResponse);
    }

    public Mono<PageResponse<SiteResponse>> searchSites(String status, String region, String name, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        return Mono.fromCallable(() -> {
            Specification<Site> spec = buildSpecification(status, region, name);
            Page<Site> result = siteRepository.findAll(spec, PageRequest.of(safePage, safeSize, Sort.by("name")));
            List<SiteResponse> content = result.getContent().stream().map(SiteMapper::toResponse).toList();
            return new PageResponse<>(content, safePage, safeSize, result.getTotalElements());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Specification<Site> buildSpecification(String status, String region, String name) {
        Specification<Site> spec = (root, query, cb) -> cb.conjunction();
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (region != null && !region.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("region"), region));
        }
        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }
        return spec;
    }

    public Mono<SiteDetailResponse> getSite(String siteId) {
        return cacheService.get(siteId)
                .switchIfEmpty(loadDetail(siteId)
                        .flatMap(detail -> cacheService.put(siteId, detail).thenReturn(detail)));
    }

    public Mono<SiteDetailResponse> updateSite(String siteId, SiteRequest request) {
        return Mono.fromCallable(() -> {
            Site site = siteRepository.findById(siteId).orElseThrow(() -> NotFoundException.site(siteId));
            site.setName(request.name());
            site.setRegion(request.region());
            site.setStatus(request.status());
            site.setUpdatedAt(Instant.now());
            siteRepository.save(site);
            if (request.wanCircuits() != null) {
                wanCircuitRepository.deleteBySiteId(siteId);
                saveWanCircuits(siteId, request.wanCircuits());
            }
            return site;
        }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(site -> cacheService.evict(siteId).then(loadDetail(siteId)))
                .flatMap(detail -> cacheService.put(siteId, detail).thenReturn(detail));
    }

    public Mono<Void> deleteSite(String siteId) {
        return Mono.fromCallable(() -> {
            if (!siteRepository.existsById(siteId)) {
                throw NotFoundException.site(siteId);
            }
            List<NetworkProfile> profiles = networkProfileRepository.findBySiteId(siteId);
            for (NetworkProfile p : profiles) {
                vlanRepository.deleteByNetworkProfileId(p.getId());
                subnetRepository.deleteByNetworkProfileId(p.getId());
            }
            networkProfileRepository.deleteBySiteId(siteId);
            deviceRepository.deleteBySiteId(siteId);
            wanCircuitRepository.deleteBySiteId(siteId);
            siteRepository.deleteById(siteId);
            return true;
        }).subscribeOn(Schedulers.boundedElastic())
                .then(cacheService.evict(siteId));
    }

    /**
     * Reconciles the site profile against the Phase 1 mock source systems
     * (master spec §22): location-service for site fields, inventory-service
     * for devices (full replace), network-service for network
     * profiles/VLANs/subnets/WAN circuits (full replace). Any source that's
     * unavailable leaves its section untouched and adds a warning — refresh
     * never fails outright just because one synthetic upstream is down.
     */
    public Mono<RefreshResult> refreshSite(String siteId) {
        return Mono.fromCallable(() -> siteRepository.existsById(siteId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(NotFoundException.site(siteId));
                    }
                    return Mono.zip(
                            locationServiceClient.fetchLocation(siteId),
                            inventoryServiceClient.fetchInventory(siteId),
                            networkServiceClient.fetchNetwork(siteId)
                    );
                })
                .flatMap(tuple -> Mono.fromCallable(() -> {
                    List<String> warnings = new ArrayList<>();
                    Site site = siteRepository.findById(siteId).orElseThrow(() -> NotFoundException.site(siteId));

                    tuple.getT1().ifPresentOrElse(loc -> {
                        site.setName(loc.name() != null ? loc.name() : site.getName());
                        site.setRegion(loc.region() != null ? loc.region() : site.getRegion());
                        site.setStatus(loc.status() != null ? loc.status() : site.getStatus());
                    }, () -> warnings.add("location-service unavailable; site fields not updated"));

                    tuple.getT2().ifPresentOrElse(inv -> {
                        deviceRepository.deleteBySiteId(siteId);
                        for (var d : inv.devices()) {
                            Device device = new Device();
                            device.setId(IdGenerator.next("DEV"));
                            device.setSiteId(siteId);
                            device.setType(d.type());
                            device.setVendor(d.vendor());
                            device.setModel(d.model());
                            device.setSerialNumber(d.serialNumber());
                            device.setStatus(d.status());
                            device.setCreatedAt(Instant.now());
                            deviceRepository.save(device);
                        }
                    }, () -> warnings.add("inventory-service unavailable; devices not updated"));

                    tuple.getT3().ifPresentOrElse(net -> {
                        List<NetworkProfile> existingProfiles = networkProfileRepository.findBySiteId(siteId);
                        for (NetworkProfile p : existingProfiles) {
                            vlanRepository.deleteByNetworkProfileId(p.getId());
                            subnetRepository.deleteByNetworkProfileId(p.getId());
                        }
                        networkProfileRepository.deleteBySiteId(siteId);
                        wanCircuitRepository.deleteBySiteId(siteId);

                        for (var pr : net.networkProfiles()) {
                            NetworkProfile profile = new NetworkProfile();
                            profile.setId(IdGenerator.next("NP"));
                            profile.setSiteId(siteId);
                            profile.setProfileName(pr.profileName());
                            profile.setCreatedAt(Instant.now());
                            networkProfileRepository.save(profile);
                            for (var v : pr.vlans()) {
                                Vlan vlan = new Vlan();
                                vlan.setId(IdGenerator.next("VLAN"));
                                vlan.setNetworkProfileId(profile.getId());
                                vlan.setVlanTag(v.vlanTag());
                                vlan.setName(v.name());
                                vlanRepository.save(vlan);
                            }
                            for (var s : pr.subnets()) {
                                Subnet subnet = new Subnet();
                                subnet.setId(IdGenerator.next("SUB"));
                                subnet.setNetworkProfileId(profile.getId());
                                subnet.setCidr(s.cidr());
                                subnet.setPurpose(s.purpose());
                                subnetRepository.save(subnet);
                            }
                        }
                        for (var c : net.wanCircuits()) {
                            WanCircuit circuit = new WanCircuit();
                            circuit.setId(IdGenerator.next("WAN"));
                            circuit.setSiteId(siteId);
                            circuit.setCarrier(c.carrier());
                            circuit.setCircuitId(c.circuitId());
                            circuit.setBandwidthMbps(c.bandwidthMbps());
                            wanCircuitRepository.save(circuit);
                        }
                    }, () -> warnings.add("network-service unavailable; network profiles/WAN circuits not updated"));

                    site.setSourceSystem("location-service,inventory-service,network-service");
                    site.setLastRefreshedAt(Instant.now());
                    site.setUpdatedAt(Instant.now());
                    siteRepository.save(site);
                    return warnings;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(warnings -> cacheService.evict(siteId).then(loadDetail(siteId))
                        .flatMap(detail -> cacheService.put(siteId, detail).thenReturn(new RefreshResult(detail, warnings))));
    }

    private void saveWanCircuits(String siteId, List<com.ensap.siteprofile.dto.WanCircuitRequest> circuits) {
        if (circuits == null) {
            return;
        }
        for (var c : circuits) {
            WanCircuit wc = new WanCircuit();
            wc.setId(IdGenerator.next("WAN"));
            wc.setSiteId(siteId);
            wc.setCarrier(c.carrier());
            wc.setCircuitId(c.circuitId());
            wc.setBandwidthMbps(c.bandwidthMbps());
            wanCircuitRepository.save(wc);
        }
    }

    private Mono<SiteDetailResponse> loadDetail(String siteId) {
        return Mono.fromCallable(() -> {
            Site site = siteRepository.findById(siteId).orElseThrow(() -> NotFoundException.site(siteId));
            List<Device> devices = deviceRepository.findBySiteId(siteId);
            List<NetworkProfile> profiles = networkProfileRepository.findBySiteId(siteId);
            List<String> profileIds = profiles.stream().map(NetworkProfile::getId).toList();
            List<Vlan> vlans = profileIds.isEmpty() ? List.of() : vlanRepository.findByNetworkProfileIdIn(profileIds);
            List<Subnet> subnets = profileIds.isEmpty() ? List.of() : subnetRepository.findByNetworkProfileIdIn(profileIds);
            List<WanCircuit> circuits = wanCircuitRepository.findBySiteId(siteId);
            return SiteMapper.toDetail(site, devices, profiles, vlans, subnets, circuits);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public record RefreshResult(SiteDetailResponse detail, List<String> warnings) {
    }
}
