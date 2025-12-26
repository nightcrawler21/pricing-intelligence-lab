package com.example.pricinglab.common.enums;

/**
 * Types of auditable actions in the system.
 */
public enum AuditAction {

    // Experiment lifecycle
    EXPERIMENT_CREATED,
    EXPERIMENT_UPDATED,
    EXPERIMENT_SUBMITTED,
    EXPERIMENT_APPROVED,
    EXPERIMENT_REJECTED,
    EXPERIMENT_CANCELLED,

    // Simulation lifecycle
    SIMULATION_STARTED,
    SIMULATION_COMPLETED,
    SIMULATION_FAILED,

    // Data modifications
    GUARDRAILS_UPDATED,
    SCOPE_UPDATED,
    LEVER_ADDED,
    LEVER_REMOVED
}
