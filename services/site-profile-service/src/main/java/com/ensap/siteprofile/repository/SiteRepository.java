package com.ensap.siteprofile.repository;

import com.ensap.siteprofile.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link Site} (docs/06-component-design.md — Repository layer).
 * Phase 0: no query methods beyond CRUD are needed until Phase 1 implements
 * search/filtering.
 */
public interface SiteRepository extends JpaRepository<Site, String> {
}
