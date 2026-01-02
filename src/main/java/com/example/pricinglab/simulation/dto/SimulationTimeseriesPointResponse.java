package com.example.pricinglab.simulation.dto;

import java.time.LocalDate;

/**
 * Response DTO for a single timeseries data point.
 */
public record SimulationTimeseriesPointResponse(
        LocalDate date,
        VariantMetrics control,
        VariantMetrics test,
        DeltaMetrics delta
) {}
