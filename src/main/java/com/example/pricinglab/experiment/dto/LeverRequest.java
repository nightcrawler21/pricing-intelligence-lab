package com.example.pricinglab.experiment.dto;

import com.example.pricinglab.common.enums.LeverType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for configuring an experiment's pricing lever.
 *
 * v0 supports only PRICE_DISCOUNT lever type with a discount percentage.
 */
public record LeverRequest(
    @NotNull(message = "type is required")
    LeverType type,

    @NotNull(message = "skuId is required")
    UUID skuId,

    @NotNull(message = "discountPercentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "discountPercentage must be greater than 0")
    @DecimalMax(value = "50.0", inclusive = true, message = "discountPercentage must not exceed 50")
    BigDecimal discountPercentage
) {}
