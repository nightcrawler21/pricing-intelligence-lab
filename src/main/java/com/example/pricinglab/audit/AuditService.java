package com.example.pricinglab.audit;

import com.example.pricinglab.common.enums.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for creating and querying audit log entries.
 *
 * All significant actions in the system should be logged through this service
 * to maintain a complete audit trail.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Logs an audit event.
     * Uses REQUIRES_NEW propagation to ensure audit logs are persisted
     * even if the main transaction rolls back.
     *
     * @param entityType the type of entity being audited
     * @param entityId the ID of the entity
     * @param action the action being performed
     * @param notes optional notes about the action
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String entityType, UUID entityId, AuditAction action, String notes) {
        String username = getCurrentUsername();

        AuditLog auditLog = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .performedBy(username)
            .notes(notes)
            .build();

        auditRepository.save(auditLog);
        log.debug("Audit log created: {} {} {} by {}", action, entityType, entityId, username);
    }

    /**
     * Logs an audit event with before/after values.
     *
     * @param entityType the type of entity being audited
     * @param entityId the ID of the entity
     * @param action the action being performed
     * @param oldValue JSON representation of previous state
     * @param newValue JSON representation of new state
     * @param notes optional notes
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(
        String entityType,
        UUID entityId,
        AuditAction action,
        String oldValue,
        String newValue,
        String notes
    ) {
        String username = getCurrentUsername();

        AuditLog auditLog = AuditLog.builder()
            .entityType(entityType)
            .entityId(entityId)
            .action(action)
            .performedBy(username)
            .oldValue(oldValue)
            .newValue(newValue)
            .notes(notes)
            .build();

        auditRepository.save(auditLog);
        log.debug("Audit log created: {} {} {} by {}", action, entityType, entityId, username);
    }

    /**
     * Retrieves audit history for an entity.
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit log entries, newest first
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId);
    }

    /**
     * Retrieves all audit logs for a specific entity ID across all entity types.
     *
     * @param entityId the entity ID
     * @return list of audit log entries, newest first
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getHistoryById(UUID entityId) {
        return auditRepository.findByEntityIdOrderByPerformedAtDesc(entityId);
    }

    /**
     * Retrieves audit logs for actions performed by a specific user.
     *
     * @param username the username
     * @return list of audit log entries, newest first
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserActivity(String username) {
        return auditRepository.findByPerformedByOrderByPerformedAtDesc(username);
    }

    /**
     * Gets the current authenticated username.
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }
}
