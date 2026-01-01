package com.example.pricinglab.experiment.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing the list of scope entries for an experiment.
 */
public record ScopeListResponse(
    UUID experimentId,
    List<ScopeEntryDto> scopes,
    int totalCount
) {
    /**
     * DTO for a single scope entry in the response.
     */
    public record ScopeEntryDto(
        UUID id,
        UUID storeId,
        String storeCode,
        String storeName,
        UUID skuId,
        String skuCode,
        String skuName,
        boolean testGroup
    ) {}
}
