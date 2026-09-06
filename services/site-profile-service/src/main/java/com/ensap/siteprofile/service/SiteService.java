package com.ensap.siteprofile.service;

import com.ensap.siteprofile.dto.SiteResponse;
import com.ensap.siteprofile.repository.SiteRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Business logic for site profiles (docs/06-component-design.md — Service
 * layer). Phase 0: method signatures only — search/get/refresh business
 * logic is implemented in Phase 1 (docs/02-functional-requirements.md, FR-1).
 */
@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public Flux<SiteResponse> listSites() {
        throw new UnsupportedOperationException("listSites: implemented in Phase 1");
    }

    public Mono<SiteResponse> getSite(String siteId) {
        throw new UnsupportedOperationException("getSite: implemented in Phase 1");
    }

    public Mono<SiteResponse> refreshSite(String siteId) {
        throw new UnsupportedOperationException("refreshSite: implemented in Phase 1");
    }
}
