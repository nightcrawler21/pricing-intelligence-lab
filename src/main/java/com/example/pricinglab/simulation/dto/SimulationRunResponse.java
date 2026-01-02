package com.example.pricinglab.simulation.dto;

import com.example.pricinglab.common.enums.SimulationStatus;

import java.util.UUID;

/**
 * Response DTO for simulation run operations.
 *
 * @param simulationRunId the unique ID of the simulation run
 * @param status the current status of the simulation (RUNNING, COMPLETED, FAILED)
 * @param totalDaysSimulated the number of days in the simulation period
 */
public record SimulationRunResponse(
        UUID simulationRunId,
        SimulationStatus status,
        Integer totalDaysSimulated
) {
}
