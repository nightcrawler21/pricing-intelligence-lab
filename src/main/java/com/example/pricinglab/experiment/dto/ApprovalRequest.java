package com.example.pricinglab.experiment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for approving or rejecting an experiment.
 */
public record ApprovalRequest(

    @NotNull(message = "Approval decision is required")
    Boolean approved,

    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    String rejectionReason
) {}
