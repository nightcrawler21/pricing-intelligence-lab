package com.example.pricinglab.experiment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for configuring experiment guardrails (v0).
 *
 * v0 guardrails use simple price floor/ceiling constraints and max change percent.
 */
public record GuardrailsRequest(
    @NotNull(message = "priceFloor is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "priceFloor must be greater than 0")
    BigDecimal priceFloor,

    @NotNull(message = "priceCeiling is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "priceCeiling must be greater than 0")
    BigDecimal priceCeiling,

    @NotNull(message = "maxChangePercent is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "maxChangePercent must be greater than 0")
    @DecimalMax(value = "50.0", inclusive = true, message = "maxChangePercent must not exceed 50")
    BigDecimal maxChangePercent
) {}
