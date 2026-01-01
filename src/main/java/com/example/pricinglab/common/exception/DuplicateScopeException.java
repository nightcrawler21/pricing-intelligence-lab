package com.example.pricinglab.common.exception;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Exception thrown when attempting to add duplicate scope entries to an experiment.
 *
 * Contains details about which store-SKU pairs are duplicates, allowing the client
 * to identify and correct the conflicting entries.
 */
public class DuplicateScopeException extends RuntimeException {

    private final List<DuplicateEntry> duplicateEntries;

    public DuplicateScopeException(List<DuplicateEntry> duplicateEntries) {
        super(buildMessage(duplicateEntries));
        this.duplicateEntries = duplicateEntries;
    }

    private static String buildMessage(List<DuplicateEntry> entries) {
        String pairs = entries.stream()
            .map(e -> String.format("(storeId=%s, skuId=%s)", e.storeId(), e.skuId()))
            .collect(Collectors.joining(", "));
        return String.format("Duplicate scope entries detected: %s", pairs);
    }

    public List<DuplicateEntry> getDuplicateEntries() {
        return duplicateEntries;
    }

    /**
     * Record representing a duplicate store-SKU pair.
     */
    public record DuplicateEntry(
        UUID storeId,
        UUID skuId,
        DuplicateType type
    ) {}

    /**
     * Type of duplicate: within the request payload or already persisted.
     */
    public enum DuplicateType {
        /**
         * Duplicate within the request payload itself.
         */
        IN_REQUEST,
        /**
         * Already exists in the experiment's persisted scope.
         */
        ALREADY_EXISTS
    }
}
