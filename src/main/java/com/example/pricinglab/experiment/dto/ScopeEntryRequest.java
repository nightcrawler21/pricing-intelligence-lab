package com.example.pricinglab.experiment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for a single scope entry (store-SKU pair).
 */
public record ScopeEntryRequest(
    @NotNull(message = "storeId is required")
    UUID storeId,

    @NotNull(message = "skuId is required")
    UUID skuId,

    /**
     * Optional flag to indicate if this entry is in the test group.
     * Defaults to true if not specified (most entries are test group entries).
     */
    Boolean testGroup
) {
    /**
     * Returns whether this entry is a test group entry.
     * Defaults to true if testGroup was not specified.
     */
    public boolean isTestGroup() {
        return testGroup == null || testGroup;
    }
}
