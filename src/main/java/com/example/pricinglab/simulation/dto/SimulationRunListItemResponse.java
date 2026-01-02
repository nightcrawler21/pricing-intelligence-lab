package com.example.pricinglab.simulation.dto;

import com.example.pricinglab.common.enums.SimulationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for simulation run list items.
 */
public record SimulationRunListItemResponse(
        UUID runId,
        SimulationStatus status,
        Instant startedAt,
        Instant completedAt,
        Integer totalDaysSimulated,
        String errorMessage
) {}
