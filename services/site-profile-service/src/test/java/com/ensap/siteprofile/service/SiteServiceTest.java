package com.ensap.siteprofile.service;

import com.ensap.siteprofile.cache.SiteCacheService;
import com.ensap.siteprofile.client.InventoryServiceClient;
import com.ensap.siteprofile.client.LocationServiceClient;
import com.ensap.siteprofile.client.NetworkServiceClient;
import com.ensap.siteprofile.dto.SiteRequest;
import com.ensap.siteprofile.entity.Site;
import com.ensap.siteprofile.exception.NotFoundException;
import com.ensap.siteprofile.repository.DeviceRepository;
import com.ensap.siteprofile.repository.NetworkProfileRepository;
import com.ensap.siteprofile.repository.SiteRepository;
import com.ensap.siteprofile.repository.SubnetRepository;
import com.ensap.siteprofile.repository.VlanRepository;
import com.ensap.siteprofile.repository.WanCircuitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the create/search/refresh business logic (docs/02-functional-requirements.md
 * FR-1) with every collaborator mocked — no Spring context, no DB.
 * End-to-end behavior against real Postgres/Redis is covered by
 * {@code SiteProfileEndToEndIT}.
 */
@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    @Mock private SiteRepository siteRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private NetworkProfileRepository networkProfileRepository;
    @Mock private VlanRepository vlanRepository;
    @Mock private SubnetRepository subnetRepository;
    @Mock private WanCircuitRepository wanCircuitRepository;
    @Mock private SiteCacheService cacheService;
    @Mock private LocationServiceClient locationServiceClient;
    @Mock private InventoryServiceClient inventoryServiceClient;
    @Mock private NetworkServiceClient networkServiceClient;

    private SiteService siteService;

    @BeforeEach
    void setUp() {
        siteService = new SiteService(siteRepository, deviceRepository, networkProfileRepository, vlanRepository,
                subnetRepository, wanCircuitRepository, cacheService, locationServiceClient, inventoryServiceClient,
                networkServiceClient);
        lenient().when(cacheService.evict(anyString())).thenReturn(Mono.empty());
        lenient().when(cacheService.put(anyString(), any())).thenReturn(Mono.empty());
        lenient().when(cacheService.get(anyString())).thenReturn(Mono.empty());
        lenient().when(deviceRepository.findBySiteId(anyString())).thenReturn(List.of());
        lenient().when(networkProfileRepository.findBySiteId(anyString())).thenReturn(List.of());
        lenient().when(wanCircuitRepository.findBySiteId(anyString())).thenReturn(List.of());
    }

    @Test
    void createSite_generatesIdWhenAbsentAndSaves() {
        when(siteRepository.existsById(any())).thenReturn(false);
        when(siteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SiteRequest request = new SiteRequest(null, "Branch A", "us-east", "ACTIVE", null);

        StepVerifier.create(siteService.createSite(request))
                .assertNext(response -> {
                    assertThat(response.id()).startsWith("SITE-");
                    assertThat(response.name()).isEqualTo("Branch A");
                    assertThat(response.status()).isEqualTo("ACTIVE");
                })
                .verifyComplete();
    }

    @Test
    void createSite_rejectsDuplicateId() {
        when(siteRepository.existsById("SITE-DUP")).thenReturn(true);

        SiteRequest request = new SiteRequest("SITE-DUP", "Branch A", "us-east", "ACTIVE", null);

        StepVerifier.create(siteService.createSite(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void searchSites_appliesPagingAndMapsContent() {
        Site site = site("SITE-001");
        Page<Site> page = new PageImpl<>(List.of(site));
        when(siteRepository.findAll(any(Specification.class), any())).thenReturn(page);

        StepVerifier.create(siteService.searchSites("ACTIVE", null, null, 0, 20))
                .assertNext(result -> {
                    assertThat(result.content()).hasSize(1);
                    assertThat(result.content().get(0).id()).isEqualTo("SITE-001");
                    assertThat(result.totalElements()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void refreshSite_notFound_errorsWithoutCallingSources() {
        when(siteRepository.existsById("SITE-MISSING")).thenReturn(false);

        StepVerifier.create(siteService.refreshSite("SITE-MISSING"))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void refreshSite_addsWarningPerUnavailableSource() {
        when(siteRepository.existsById("SITE-001")).thenReturn(true);
        when(siteRepository.findById("SITE-001")).thenReturn(Optional.of(site("SITE-001")));
        when(siteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(locationServiceClient.fetchLocation("SITE-001")).thenReturn(Mono.just(Optional.empty()));
        when(inventoryServiceClient.fetchInventory("SITE-001")).thenReturn(Mono.just(Optional.empty()));
        when(networkServiceClient.fetchNetwork("SITE-001")).thenReturn(Mono.just(Optional.empty()));

        StepVerifier.create(siteService.refreshSite("SITE-001"))
                .assertNext(result -> assertThat(result.warnings()).hasSize(3))
                .verifyComplete();
    }

    private Site site(String id) {
        Site site = new Site();
        site.setId(id);
        site.setName("Branch A");
        site.setRegion("us-east");
        site.setStatus("ACTIVE");
        site.setCreatedAt(Instant.now());
        site.setUpdatedAt(Instant.now());
        return site;
    }
}
