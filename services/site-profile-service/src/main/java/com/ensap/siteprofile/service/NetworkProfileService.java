package com.ensap.siteprofile.service;

import com.ensap.siteprofile.cache.SiteCacheService;
import com.ensap.siteprofile.dto.NetworkProfileRequest;
import com.ensap.siteprofile.dto.NetworkProfileResponse;
import com.ensap.siteprofile.entity.NetworkProfile;
import com.ensap.siteprofile.entity.Subnet;
import com.ensap.siteprofile.entity.Vlan;
import com.ensap.siteprofile.exception.NotFoundException;
import com.ensap.siteprofile.repository.NetworkProfileRepository;
import com.ensap.siteprofile.repository.SiteRepository;
import com.ensap.siteprofile.repository.SubnetRepository;
import com.ensap.siteprofile.repository.VlanRepository;
import com.ensap.siteprofile.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;

/**
 * CRUD for network profiles (with embedded VLANs/subnets) owned by a site
 * (docs/06-component-design.md). Updates replace the VLAN/subnet child rows
 * wholesale rather than diffing — simplest correct behavior for Phase 1.
 */
@Service
public class NetworkProfileService {

    private final NetworkProfileRepository networkProfileRepository;
    private final VlanRepository vlanRepository;
    private final SubnetRepository subnetRepository;
    private final SiteRepository siteRepository;
    private final SiteCacheService cacheService;

    public NetworkProfileService(NetworkProfileRepository networkProfileRepository, VlanRepository vlanRepository,
                                  SubnetRepository subnetRepository, SiteRepository siteRepository,
                                  SiteCacheService cacheService) {
        this.networkProfileRepository = networkProfileRepository;
        this.vlanRepository = vlanRepository;
        this.subnetRepository = subnetRepository;
        this.siteRepository = siteRepository;
        this.cacheService = cacheService;
    }

    public Mono<List<NetworkProfileResponse>> listProfiles(String siteId) {
        return requireSite(siteId)
                .then(Mono.fromCallable(() -> networkProfileRepository.findBySiteId(siteId)
                                .stream().map(this::toResponseBlocking).toList())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    public Mono<NetworkProfileResponse> getProfile(String siteId, String profileId) {
        return findOwned(siteId, profileId)
                .map(this::toResponseBlocking);
    }

    public Mono<NetworkProfileResponse> createProfile(String siteId, NetworkProfileRequest request) {
        return requireSite(siteId)
                .then(Mono.fromCallable(() -> {
                    NetworkProfile profile = new NetworkProfile();
                    profile.setId(IdGenerator.next("NP"));
                    profile.setSiteId(siteId);
                    profile.setProfileName(request.profileName());
                    profile.setCreatedAt(Instant.now());
                    networkProfileRepository.save(profile);
                    saveChildren(profile.getId(), request);
                    return profile;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(profile -> cacheService.evict(siteId).thenReturn(toResponseBlocking(profile)));
    }

    public Mono<NetworkProfileResponse> updateProfile(String siteId, String profileId, NetworkProfileRequest request) {
        return findOwned(siteId, profileId)
                .flatMap(profile -> Mono.fromCallable(() -> {
                    profile.setProfileName(request.profileName());
                    networkProfileRepository.save(profile);
                    vlanRepository.deleteByNetworkProfileId(profileId);
                    subnetRepository.deleteByNetworkProfileId(profileId);
                    saveChildren(profileId, request);
                    return profile;
                }).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(profile -> cacheService.evict(siteId).thenReturn(toResponseBlocking(profile)));
    }

    public Mono<Void> deleteProfile(String siteId, String profileId) {
        return findOwned(siteId, profileId)
                .flatMap(profile -> Mono.fromRunnable(() -> {
                    vlanRepository.deleteByNetworkProfileId(profileId);
                    subnetRepository.deleteByNetworkProfileId(profileId);
                    networkProfileRepository.deleteById(profileId);
                }).subscribeOn(Schedulers.boundedElastic()).then())
                .then(cacheService.evict(siteId));
    }

    private void saveChildren(String profileId, NetworkProfileRequest request) {
        if (request.vlans() != null) {
            for (var v : request.vlans()) {
                Vlan vlan = new Vlan();
                vlan.setId(IdGenerator.next("VLAN"));
                vlan.setNetworkProfileId(profileId);
                vlan.setVlanTag(v.vlanTag());
                vlan.setName(v.name());
                vlanRepository.save(vlan);
            }
        }
        if (request.subnets() != null) {
            for (var s : request.subnets()) {
                Subnet subnet = new Subnet();
                subnet.setId(IdGenerator.next("SUB"));
                subnet.setNetworkProfileId(profileId);
                subnet.setCidr(s.cidr());
                subnet.setPurpose(s.purpose());
                subnetRepository.save(subnet);
            }
        }
    }

    /** Must run on the same blocking thread as the save — called only from within a boundedElastic Mono. */
    private NetworkProfileResponse toResponseBlocking(NetworkProfile profile) {
        List<Vlan> vlans = vlanRepository.findByNetworkProfileId(profile.getId());
        List<Subnet> subnets = subnetRepository.findByNetworkProfileId(profile.getId());
        return new NetworkProfileResponse(profile.getId(), profile.getSiteId(), profile.getProfileName(),
                profile.getCreatedAt(), vlans.stream().map(v -> new com.ensap.siteprofile.dto.VlanResponse(v.getId(), v.getVlanTag(), v.getName())).toList(),
                subnets.stream().map(s -> new com.ensap.siteprofile.dto.SubnetResponse(s.getId(), s.getCidr(), s.getPurpose())).toList());
    }

    private Mono<Void> requireSite(String siteId) {
        return Mono.fromCallable(() -> siteRepository.existsById(siteId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> exists ? Mono.<Void>empty() : Mono.error(NotFoundException.site(siteId)));
    }

    private Mono<NetworkProfile> findOwned(String siteId, String profileId) {
        return Mono.fromCallable(() -> networkProfileRepository.findById(profileId)
                        .filter(p -> p.getSiteId().equals(siteId))
                        .orElseThrow(() -> NotFoundException.networkProfile(siteId, profileId)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
