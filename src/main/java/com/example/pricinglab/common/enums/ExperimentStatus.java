package com.example.pricinglab.common.enums;

/**
 * Represents the lifecycle status of a pricing experiment.
 *
 * Experiment workflow:
 * DRAFT -> PENDING_APPROVAL -> APPROVED -> RUNNING -> COMPLETED
 *                           -> REJECTED (terminal)
 *                                      -> CANCELLED (from any active state)
 */
public enum ExperimentStatus {

    /**
     * Initial state. Experiment is being configured by analyst.
     */
    DRAFT,

    /**
     * Experiment has been submitted and awaiting admin approval.
     */
    PENDING_APPROVAL,

    /**
     * Experiment has been approved and is ready for simulation.
     */
    APPROVED,

    /**
     * Simulation is currently running for this experiment.
     */
    RUNNING,

    /**
     * Experiment simulation has completed. Results are available.
     */
    COMPLETED,

    /**
     * Experiment was rejected by admin. Terminal state.
     */
    REJECTED,

    /**
     * Experiment was cancelled. Terminal state.
     */
    CANCELLED
}
