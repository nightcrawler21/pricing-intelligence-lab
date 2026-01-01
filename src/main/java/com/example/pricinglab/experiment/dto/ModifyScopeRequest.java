package com.example.pricinglab.experiment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for adding or removing scope entries (bulk operation).
 *
 * Used for both POST (add) and DELETE (remove) operations on experiment scope.
 */
public record ModifyScopeRequest(
    @NotEmpty(message = "entries list cannot be empty")
    @Valid
    List<ScopeEntryRequest> entries
) {}
