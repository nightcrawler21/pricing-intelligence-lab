package com.example.pricinglab.simulation.dto;

import com.example.pricinglab.common.enums.SimulationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for simulation run status.
 */
public record SimulationRunStatusResponse(
        UUID runId,
        UUID experimentId,
        SimulationStatus status,
        Instant startedAt,
        Instant completedAt,
        String errorMessage
) {}
