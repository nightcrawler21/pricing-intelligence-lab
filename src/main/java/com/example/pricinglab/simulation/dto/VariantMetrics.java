package com.example.pricinglab.simulation.dto;

import java.math.BigDecimal;

/**
 * Metrics for a single variant (CONTROL or TEST).
 */
public record VariantMetrics(
        BigDecimal units,
        BigDecimal revenue,
        BigDecimal margin
) {
    public static VariantMetrics zero() {
        return new VariantMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
