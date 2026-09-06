package com.ensap.evidenceaudit.repository;

import com.ensap.evidenceaudit.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link AuditEvent} (docs/06-component-design.md —
 * Repository layer). Phase 0: CRUD only; filtering added when Phase 2
 * implements {@code GET /api/audit-events}.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
}
