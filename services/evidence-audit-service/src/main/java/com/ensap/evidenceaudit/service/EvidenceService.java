package com.ensap.evidenceaudit.service;

import com.ensap.evidenceaudit.dto.EvidenceResponse;
import com.ensap.evidenceaudit.repository.DeploymentEvidenceRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Business logic for deployment evidence (docs/06-component-design.md —
 * Service layer). Phase 0: method signature only — storing/listing evidence
 * (with bodies in S3, docs/18-adr/0007) is implemented starting Phase 2
 * (docs/02-functional-requirements.md, FR-4).
 */
@Service
public class EvidenceService {

    private final DeploymentEvidenceRepository deploymentEvidenceRepository;

    public EvidenceService(DeploymentEvidenceRepository deploymentEvidenceRepository) {
        this.deploymentEvidenceRepository = deploymentEvidenceRepository;
    }

    public Flux<EvidenceResponse> listEvidence(String deploymentId) {
        throw new UnsupportedOperationException("listEvidence: implemented in Phase 2");
    }
}
