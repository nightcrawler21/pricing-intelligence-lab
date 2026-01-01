package com.example.pricinglab.common.enums;

/**
 * Represents the lifecycle status of a pricing experiment.
 *
 * <p>Allowed state transitions:</p>
 * <ul>
 *   <li>DRAFT → PENDING_APPROVAL (via submit)</li>
 *   <li>PENDING_APPROVAL → APPROVED (via approve)</li>
 *   <li>PENDING_APPROVAL → REJECTED (via reject)</li>
 *   <li>APPROVED → RUNNING (via run simulation)</li>
 *   <li>RUNNING → COMPLETED (via complete simulation)</li>
 *   <li>RUNNING → FAILED (via fail simulation)</li>
 * </ul>
 *
 * <p>Terminal states: COMPLETED, FAILED, REJECTED</p>
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
     * Experiment simulation has completed successfully. Results are available.
     */
    COMPLETED,

    /**
     * Experiment simulation failed. Terminal state.
     */
    FAILED,

    /**
     * Experiment was rejected by admin. Terminal state.
     */
    REJECTED
}
