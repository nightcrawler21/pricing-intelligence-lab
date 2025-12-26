package com.example.pricinglab.audit;

import com.example.pricinglab.common.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit log entry for tracking all significant actions in the system.
 *
 * Audit logs are append-only and cannot be modified or deleted.
 * They provide a complete audit trail for compliance and debugging.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_user", columnList = "performed_by"),
    @Index(name = "idx_audit_timestamp", columnList = "performed_at")
})
public class AuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Type of entity being audited (e.g., "Experiment", "SimulationRun").
     */
    @Column(name = "entity_type", nullable = false, updatable = false, length = 100)
    private String entityType;

    /**
     * ID of the entity being audited.
     */
    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    /**
     * The action that was performed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 50)
    private AuditAction action;

    /**
     * Username of the user who performed the action.
     */
    @Column(name = "performed_by", nullable = false, updatable = false, length = 100)
    private String performedBy;

    /**
     * Timestamp when the action was performed.
     */
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    /**
     * JSON representation of the entity state before the action (for updates/deletes).
     */
    @Column(name = "old_value", updatable = false, columnDefinition = "TEXT")
    private String oldValue;

    /**
     * JSON representation of the entity state after the action (for creates/updates).
     */
    @Column(name = "new_value", updatable = false, columnDefinition = "TEXT")
    private String newValue;

    /**
     * Additional context or notes about the action.
     */
    @Column(name = "notes", updatable = false, length = 1000)
    private String notes;

    /**
     * IP address of the user (if available).
     */
    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    protected AuditLog() {
        // JPA
    }

    private AuditLog(Builder builder) {
        this.id = UUID.randomUUID();
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.action = builder.action;
        this.performedBy = builder.performedBy;
        this.performedAt = Instant.now();
        this.oldValue = builder.oldValue;
        this.newValue = builder.newValue;
        this.notes = builder.notes;
        this.ipAddress = builder.ipAddress;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters only - audit logs are immutable

    public UUID getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getNotes() {
        return notes;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
            "id=" + id +
            ", entityType='" + entityType + '\'' +
            ", entityId=" + entityId +
            ", action=" + action +
            ", performedBy='" + performedBy + '\'' +
            ", performedAt=" + performedAt +
            '}';
    }

    /**
     * Builder for creating audit log entries.
     */
    public static class Builder {
        private String entityType;
        private UUID entityId;
        private AuditAction action;
        private String performedBy;
        private String oldValue;
        private String newValue;
        private String notes;
        private String ipAddress;

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder action(AuditAction action) {
            this.action = action;
            return this;
        }

        public Builder performedBy(String performedBy) {
            this.performedBy = performedBy;
            return this;
        }

        public Builder oldValue(String oldValue) {
            this.oldValue = oldValue;
            return this;
        }

        public Builder newValue(String newValue) {
            this.newValue = newValue;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AuditLog build() {
            Objects.requireNonNull(entityType, "entityType is required");
            Objects.requireNonNull(entityId, "entityId is required");
            Objects.requireNonNull(action, "action is required");
            Objects.requireNonNull(performedBy, "performedBy is required");
            return new AuditLog(this);
        }
    }
}
