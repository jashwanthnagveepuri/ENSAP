package com.ensap.deployment.repository;

import com.ensap.deployment.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence for {@link OutboxEvent} (docs/09-database-design.md —
 * Repository layer).
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
}
