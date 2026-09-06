package com.ensap.deployment.repository;

import com.ensap.deployment.entity.DeploymentStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link DeploymentStep} (docs/06-component-design.md —
 * Repository layer).
 */
public interface DeploymentStepRepository extends JpaRepository<DeploymentStep, String> {

    List<DeploymentStep> findByDeploymentId(String deploymentId);

    Optional<DeploymentStep> findByDeploymentIdAndStepName(String deploymentId, String stepName);
}
