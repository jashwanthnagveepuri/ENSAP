package com.ensap.evidenceaudit.service;

import com.ensap.evidenceaudit.dto.AuditEventResponse;
import com.ensap.evidenceaudit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Business logic for audit events (docs/06-component-design.md — Service
 * layer). Phase 0: method signature only — recording/listing audit events
 * is implemented starting Phase 2 (docs/02-functional-requirements.md, FR-4).
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public Flux<AuditEventResponse> listAuditEvents() {
        throw new UnsupportedOperationException("listAuditEvents: implemented in Phase 2");
    }
}
