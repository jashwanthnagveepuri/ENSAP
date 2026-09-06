package com.ensap.deployment.repository;

import com.ensap.deployment.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link Deployment} (docs/06-component-design.md —
 * Repository layer). Phase 0: no query methods beyond CRUD until Phase 2
 * implements listing/filtering and idempotency-key lookup.
 */
public interface DeploymentRepository extends JpaRepository<Deployment, String> {
}
