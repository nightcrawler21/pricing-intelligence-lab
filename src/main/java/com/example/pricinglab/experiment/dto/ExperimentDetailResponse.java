package com.example.pricinglab.experiment.dto;

import com.example.pricinglab.common.enums.ExperimentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for a single experiment including scopes, levers, and guardrails.
 */
public record ExperimentDetailResponse(
    UUID id,
    String name,
    String description,
    ExperimentStatus status,
    LocalDate startDate,
    LocalDate endDate,
    String businessJustification,
    String hypothesis,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt,
    String approvedBy,
    String rejectionReason,
    List<ScopeDto> scopes,
    List<LeverDto> levers,
    GuardrailsDto guardrails
) {

    public record ScopeDto(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID skuId,
        String skuCode,
        String skuName,
        boolean testGroup
    ) {}

    public record LeverDto(
        UUID id,
        UUID skuId,
        String skuCode,
        String skuName,
        String leverType,
        java.math.BigDecimal leverValue,
        String description
    ) {}

    public record GuardrailsDto(
        java.math.BigDecimal maxDiscountPercentage,
        java.math.BigDecimal maxMarkupPercentage,
        java.math.BigDecimal minMarginPercentage,
        java.math.BigDecimal maxRevenueImpactPercentage,
        boolean preventBelowCost,
        boolean enforcePricePoints
    ) {}
}
