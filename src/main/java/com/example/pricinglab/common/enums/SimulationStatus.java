package com.example.pricinglab.common.enums;

/**
 * Status of a simulation run.
 */
public enum SimulationStatus {

    /**
     * Simulation is queued and waiting to start.
     */
    PENDING,

    /**
     * Simulation is currently processing.
     */
    RUNNING,

    /**
     * Simulation completed successfully.
     */
    COMPLETED,

    /**
     * Simulation failed due to an error.
     */
    FAILED
}
