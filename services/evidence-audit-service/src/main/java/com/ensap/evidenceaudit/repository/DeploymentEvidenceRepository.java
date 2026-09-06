package com.ensap.evidenceaudit.repository;

import com.ensap.evidenceaudit.entity.DeploymentEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link DeploymentEvidence} (docs/06-component-design.md —
 * Repository layer). Phase 0: CRUD only; lookup-by-deployment added when
 * Phase 2 implements {@code GET /api/deployments/{deploymentId}/evidence}.
 */
public interface DeploymentEvidenceRepository extends JpaRepository<DeploymentEvidence, String> {
}
