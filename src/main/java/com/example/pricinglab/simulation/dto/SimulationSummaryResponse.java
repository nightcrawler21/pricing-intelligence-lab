package com.example.pricinglab.simulation.dto;

import java.util.UUID;

/**
 * Response DTO for simulation summary with control/test/delta metrics.
 */
public record SimulationSummaryResponse(
        UUID runId,
        VariantMetrics control,
        VariantMetrics test,
        DeltaMetrics delta
) {}
