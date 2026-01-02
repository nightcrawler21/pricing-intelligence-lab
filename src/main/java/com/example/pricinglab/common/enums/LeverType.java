package com.example.pricinglab.common.enums;

/**
 * Types of pricing levers that can be applied in an experiment.
 *
 * A lever represents a specific pricing action that will be tested.
 */
public enum LeverType {

    /**
     * Price discount as a percentage off the base price.
     * Value represents discount percentage (e.g., 10.0 = 10% off).
     * v0 lever type - the only supported type initially.
     */
    PRICE_DISCOUNT,

    /**
     * Percentage-based price adjustment.
     * Example: -5% (discount) or +10% (markup)
     */
    PERCENTAGE_CHANGE,

    /**
     * Absolute price adjustment in local currency.
     * Example: -5.00 THB or +10.00 THB
     */
    ABSOLUTE_CHANGE,

    /**
     * Set a fixed target price.
     * Example: Set price to 99.00 THB
     */
    TARGET_PRICE,

    /**
     * Match competitor price (requires competitor price data).
     * Not implemented in v0.
     */
    COMPETITOR_MATCH
}
