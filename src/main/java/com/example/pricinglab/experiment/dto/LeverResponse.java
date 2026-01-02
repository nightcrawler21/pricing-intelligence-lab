package com.example.pricinglab.experiment.dto;

import com.example.pricinglab.common.enums.LeverType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for an experiment's configured lever.
 */
public record LeverResponse(
    UUID id,
    UUID experimentId,
    LeverType type,
    UUID skuId,
    String skuCode,
    String skuName,
    BigDecimal discountPercentage
) {}
