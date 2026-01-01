package com.example.pricinglab.experiment.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.exception.DuplicateScopeException;
import com.example.pricinglab.common.exception.DuplicateScopeException.DuplicateEntry;
import com.example.pricinglab.common.exception.DuplicateScopeException.DuplicateType;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import com.example.pricinglab.experiment.dto.ModifyScopeRequest;
import com.example.pricinglab.experiment.dto.ScopeEntryRequest;
import com.example.pricinglab.experiment.dto.ScopeListResponse;
import com.example.pricinglab.experiment.dto.ScopeListResponse.ScopeEntryDto;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.repository.ExperimentScopeRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing experiment scope entries.
 *
 * Handles adding and removing store-SKU pairs from experiment scope,
 * with validation, deduplication, and audit logging.
 */
@Service
@Transactional
public class ExperimentScopeService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentScopeService.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentScopeRepository scopeRepository;
    private final StoreRepository storeRepository;
    private final SkuRepository skuRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ExperimentScopeService(
            ExperimentRepository experimentRepository,
            ExperimentScopeRepository scopeRepository,
            StoreRepository storeRepository,
            SkuRepository skuRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.scopeRepository = scopeRepository;
        this.storeRepository = storeRepository;
        this.skuRepository = skuRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Adds scope entries to an experiment.
     *
     * @param experimentId the experiment ID
     * @param request the request containing entries to add
     * @return the updated scope list
     * @throws ResourceNotFoundException if experiment, store, or SKU not found
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     * @throws DuplicateScopeException if duplicate entries are detected
     */
    public ScopeListResponse addScopeEntries(UUID experimentId, ModifyScopeRequest request) {
        log.info("Adding {} scope entries to experiment {}", request.entries().size(), experimentId);

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment, "modify scope");

        // Check for duplicates within the request payload
        List<DuplicateEntry> duplicates = findDuplicatesInRequest(request.entries());

        // Collect all unique store and SKU IDs for batch validation
        Set<UUID> storeIds = request.entries().stream()
                .map(ScopeEntryRequest::storeId)
                .collect(Collectors.toSet());
        Set<UUID> skuIds = request.entries().stream()
                .map(ScopeEntryRequest::skuId)
                .collect(Collectors.toSet());

        // Validate all stores exist
        Map<UUID, Store> stores = validateAndLoadStores(storeIds);

        // Validate all SKUs exist
        Map<UUID, Sku> skus = validateAndLoadSkus(skuIds);

        // Load existing scope entries for this experiment (for duplicate checking)
        List<ExperimentScope> existingScopes = scopeRepository.findByExperimentId(experimentId);
        Set<String> existingPairs = existingScopes.stream()
                .map(s -> scopeKey(s.getStore().getId(), s.getSku().getId()))
                .collect(Collectors.toSet());

        // Check for duplicates against existing persisted entries
        for (ScopeEntryRequest entry : request.entries()) {
            String key = scopeKey(entry.storeId(), entry.skuId());
            if (existingPairs.contains(key)) {
                duplicates.add(new DuplicateEntry(entry.storeId(), entry.skuId(), DuplicateType.ALREADY_EXISTS));
            }
        }

        // If any duplicates found, reject the entire request
        if (!duplicates.isEmpty()) {
            throw new DuplicateScopeException(duplicates);
        }

        // Create and save new scope entries
        List<ExperimentScope> newScopes = new ArrayList<>();
        for (ScopeEntryRequest entry : request.entries()) {
            ExperimentScope scope = new ExperimentScope(
                    UUID.randomUUID(),
                    stores.get(entry.storeId()),
                    skus.get(entry.skuId()),
                    entry.isTestGroup()
            );
            scope.setExperiment(experiment);
            newScopes.add(scope);
        }

        scopeRepository.saveAll(newScopes);
        log.info("Added {} scope entries to experiment {}", newScopes.size(), experimentId);

        // Audit log the addition
        logScopeChange(experimentId, AuditAction.EXPERIMENT_SCOPE_ADDED, request.entries());

        // Return updated scope list
        return getScopeList(experimentId);
    }

    /**
     * Removes scope entries from an experiment.
     *
     * Behavior: Missing entries are silently ignored (bulk delete convenience).
     * This approach allows callers to request removal of entries without needing
     * to first check if they exist, simplifying client logic for bulk operations.
     *
     * @param experimentId the experiment ID
     * @param request the request containing entries to remove
     * @return the updated scope list
     * @throws ResourceNotFoundException if experiment not found
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public ScopeListResponse removeScopeEntries(UUID experimentId, ModifyScopeRequest request) {
        log.info("Removing {} scope entries from experiment {}", request.entries().size(), experimentId);

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment, "modify scope");

        // Find and remove matching entries
        List<ScopeEntryRequest> removedEntries = new ArrayList<>();
        for (ScopeEntryRequest entry : request.entries()) {
            scopeRepository.findByExperimentIdAndStoreIdAndSkuId(
                    experimentId, entry.storeId(), entry.skuId()
            ).ifPresent(scope -> {
                scopeRepository.delete(scope);
                removedEntries.add(entry);
            });
            // Note: If entry doesn't exist, we silently ignore it (bulk delete convenience)
        }

        if (!removedEntries.isEmpty()) {
            log.info("Removed {} scope entries from experiment {}", removedEntries.size(), experimentId);
            // Audit log the removal (only log actually removed entries)
            logScopeChange(experimentId, AuditAction.EXPERIMENT_SCOPE_REMOVED, removedEntries);
        } else {
            log.info("No matching scope entries found to remove from experiment {}", experimentId);
        }

        // Return updated scope list
        return getScopeList(experimentId);
    }

    /**
     * Gets the current scope list for an experiment.
     *
     * @param experimentId the experiment ID
     * @return the scope list response
     * @throws ResourceNotFoundException if experiment not found
     */
    @Transactional(readOnly = true)
    public ScopeListResponse getScopeList(UUID experimentId) {
        // Verify experiment exists
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment", experimentId.toString());
        }

        List<ExperimentScope> scopes = scopeRepository.findByExperimentId(experimentId);
        List<ScopeEntryDto> scopeDtos = scopes.stream()
                .map(this::toScopeEntryDto)
                .toList();

        return new ScopeListResponse(experimentId, scopeDtos, scopeDtos.size());
    }

    // --- Private helper methods ---

    private Experiment getExperimentOrThrow(UUID experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));
    }

    private void validateDraftStatus(Experiment experiment, String action) {
        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new InvalidExperimentStateException(
                    experiment.getStatus(),
                    action,
                    ExperimentStatus.DRAFT
            );
        }
    }

    private List<DuplicateEntry> findDuplicatesInRequest(List<ScopeEntryRequest> entries) {
        List<DuplicateEntry> duplicates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ScopeEntryRequest entry : entries) {
            String key = scopeKey(entry.storeId(), entry.skuId());
            if (!seen.add(key)) {
                duplicates.add(new DuplicateEntry(entry.storeId(), entry.skuId(), DuplicateType.IN_REQUEST));
            }
        }

        return duplicates;
    }

    private Map<UUID, Store> validateAndLoadStores(Set<UUID> storeIds) {
        List<Store> stores = storeRepository.findByIdIn(new ArrayList<>(storeIds));
        Map<UUID, Store> storeMap = stores.stream()
                .collect(Collectors.toMap(Store::getId, s -> s));

        // Check for missing stores
        Set<UUID> missingIds = new HashSet<>(storeIds);
        missingIds.removeAll(storeMap.keySet());
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Store", missingIds.iterator().next().toString());
        }

        return storeMap;
    }

    private Map<UUID, Sku> validateAndLoadSkus(Set<UUID> skuIds) {
        List<Sku> skus = skuRepository.findByIdIn(new ArrayList<>(skuIds));
        Map<UUID, Sku> skuMap = skus.stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        // Check for missing SKUs
        Set<UUID> missingIds = new HashSet<>(skuIds);
        missingIds.removeAll(skuMap.keySet());
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Sku", missingIds.iterator().next().toString());
        }

        return skuMap;
    }

    private String scopeKey(UUID storeId, UUID skuId) {
        return storeId.toString() + ":" + skuId.toString();
    }

    private ScopeEntryDto toScopeEntryDto(ExperimentScope scope) {
        return new ScopeEntryDto(
                scope.getId(),
                scope.getStore().getId(),
                scope.getStore().getStoreCode(),
                scope.getStore().getStoreName(),
                scope.getSku().getId(),
                scope.getSku().getSkuCode(),
                scope.getSku().getSkuName(),
                scope.isTestGroup()
        );
    }

    private void logScopeChange(UUID experimentId, AuditAction action, List<ScopeEntryRequest> entries) {
        Map<String, Object> details = new HashMap<>();
        details.put("count", entries.size());
        details.put("entries", entries.stream()
                .map(e -> Map.of("storeId", e.storeId().toString(), "skuId", e.skuId().toString()))
                .toList());

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details", e);
            detailsJson = String.format("{\"count\": %d}", entries.size());
        }

        auditService.logAction(
                "Experiment",
                experimentId,
                action,
                detailsJson
        );
    }
}
