package com.ensap.evidenceaudit.repository;

import com.ensap.evidenceaudit.entity.ProcessedOperation;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link ProcessedOperation} (docs/09-database-design.md —
 * Repository layer). {@code existsById} is the idempotency check the Kafka
 * consumer runs before recording an audit event (master spec §12).
 */
public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, String> {
}
