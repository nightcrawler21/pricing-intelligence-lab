package com.example.pricinglab.experiment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating a new experiment.
 */
public record CreateExperimentRequest(

    @NotBlank(message = "Experiment name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    LocalDate endDate,

    @Size(max = 2000, message = "Business justification must not exceed 2000 characters")
    String businessJustification,

    @Size(max = 1000, message = "Hypothesis must not exceed 1000 characters")
    String hypothesis
) {}
