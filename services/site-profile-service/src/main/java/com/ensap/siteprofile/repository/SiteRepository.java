package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link Site} (docs/06-component-design.md — Repository layer).
 * {@link JpaSpecificationExecutor} backs the Phase 1 search/filter endpoint
 * (status/region/name, docs/10-api-design.md pagination & filtering).
 */
public interface SiteRepository extends JpaRepository<Site, String>, JpaSpecificationExecutor<Site> {
}
