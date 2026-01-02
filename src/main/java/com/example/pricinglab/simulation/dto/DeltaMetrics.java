package com.example.pricinglab.simulation.dto;

import java.math.BigDecimal;

/**
 * Delta metrics (TEST - CONTROL) with absolute and percentage values.
 */
public record DeltaMetrics(
        BigDecimal units,
        BigDecimal revenue,
        BigDecimal margin,
        BigDecimal revenuePct,
        BigDecimal marginPct
) {
    public static DeltaMetrics zero() {
        return new DeltaMetrics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
