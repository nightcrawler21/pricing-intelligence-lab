package com.example.pricinglab.audit;

import com.example.pricinglab.common.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entities.
 *
 * Note: Only read and create operations are supported.
 * Audit logs are immutable and cannot be updated or deleted.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, UUID entityId);

    List<AuditLog> findByEntityTypeOrderByPerformedAtDesc(String entityType);

    List<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy);

    List<AuditLog> findByActionOrderByPerformedAtDesc(AuditAction action);

    List<AuditLog> findByPerformedAtBetweenOrderByPerformedAtDesc(Instant start, Instant end);

    List<AuditLog> findByEntityIdOrderByPerformedAtDesc(UUID entityId);
}
