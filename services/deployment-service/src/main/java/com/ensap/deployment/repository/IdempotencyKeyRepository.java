package com.ensap.deployment.repository;

import com.ensap.deployment.entity.IdempotencyKeyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for {@link IdempotencyKeyRecord} (docs/06-component-design.md
 * — Repository layer; master spec §12).
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyRecord, String> {
}
