package com.example.pricinglab.simulation.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a breakdown row (by store, SKU, or date).
 */
public record SimulationBreakdownRowResponse(
        UUID storeId,
        UUID skuId,
        LocalDate date,
        VariantMetrics control,
        VariantMetrics test,
        DeltaMetrics delta
) {}
