package com.example.pricinglab.experiment.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for experiment guardrails (v0).
 */
public record GuardrailsResponse(
    UUID experimentId,
    BigDecimal priceFloor,
    BigDecimal priceCeiling,
    BigDecimal maxChangePercent
) {}
