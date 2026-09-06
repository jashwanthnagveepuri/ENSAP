package com.ensap.deployment.repository;

import com.ensap.deployment.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link Deployment} (docs/06-component-design.md —
 * Repository layer). {@link JpaSpecificationExecutor} backs the
 * status/siteId filters on {@code GET /api/deployments}.
 */
public interface DeploymentRepository extends JpaRepository<Deployment, String>, JpaSpecificationExecutor<Deployment> {
}
