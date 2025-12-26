package com.example.pricinglab.experiment.dto;

import com.example.pricinglab.common.enums.ExperimentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for experiment data.
 */
public record ExperimentResponse(
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
    String approvedBy,
    String rejectionReason,
    int scopeCount,
    int leverCount,
    boolean hasGuardrails
) {}
