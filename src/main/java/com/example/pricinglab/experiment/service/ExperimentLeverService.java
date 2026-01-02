package com.example.pricinglab.experiment.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.dto.LeverRequest;
import com.example.pricinglab.experiment.dto.LeverResponse;
import com.example.pricinglab.experiment.repository.ExperimentLeverRepository;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.repository.ExperimentScopeRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing experiment levers.
 *
 * v0 supports exactly one lever per experiment with type PRICE_DISCOUNT.
 * Lever configuration is only allowed when experiment is in DRAFT status.
 */
@Service
@Transactional
public class ExperimentLeverService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentLeverService.class);

    private static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("50.0");

    private final ExperimentRepository experimentRepository;
    private final ExperimentLeverRepository leverRepository;
    private final ExperimentScopeRepository scopeRepository;
    private final SkuRepository skuRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ExperimentLeverService(
            ExperimentRepository experimentRepository,
            ExperimentLeverRepository leverRepository,
            ExperimentScopeRepository scopeRepository,
            SkuRepository skuRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.leverRepository = leverRepository;
        this.scopeRepository = scopeRepository;
        this.skuRepository = skuRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Configures (creates or replaces) the lever for an experiment.
     *
     * v0: Only one lever per experiment, type must be PRICE_DISCOUNT.
     *
     * @param experimentId the experiment ID
     * @param request the lever configuration request
     * @return the configured lever
     * @throws ResourceNotFoundException if experiment or SKU not found
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     * @throws IllegalArgumentException if validation fails (type, scope, discount range)
     */
    public LeverResponse configureLever(UUID experimentId, LeverRequest request) {
        log.info("Configuring lever for experiment {}: type={}, skuId={}, discount={}%",
                experimentId, request.type(), request.skuId(), request.discountPercentage());

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment);

        // Validate lever type (v0: only PRICE_DISCOUNT supported)
        validateLeverType(request.type());

        // Validate discount percentage range
        validateDiscountPercentage(request.discountPercentage());

        // Validate SKU exists
        Sku sku = getSkuOrThrow(request.skuId());

        // Validate SKU is in experiment scope
        validateSkuInScope(experimentId, request.skuId());

        // Check if lever already exists - if so, replace it
        ExperimentLever lever = leverRepository.findFirstByExperimentId(experimentId)
                .orElse(null);

        boolean isReplacement = lever != null;

        if (lever == null) {
            lever = new ExperimentLever(UUID.randomUUID(), sku, request.type(), request.discountPercentage());
            lever.setExperiment(experiment);
        } else {
            lever.setSku(sku);
            lever.setLeverType(request.type());
            lever.setLeverValue(request.discountPercentage());
        }

        lever = leverRepository.save(lever);

        log.info("{} lever {} for experiment {}",
                isReplacement ? "Replaced" : "Created", lever.getId(), experimentId);

        // Audit log
        logLeverSet(experimentId, lever);

        return toLeverResponse(lever, experimentId);
    }

    /**
     * Removes the lever from an experiment.
     *
     * @param experimentId the experiment ID
     * @throws ResourceNotFoundException if experiment not found or no lever exists
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public void removeLever(UUID experimentId) {
        log.info("Removing lever from experiment {}", experimentId);

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment);

        // Find existing lever
        ExperimentLever lever = leverRepository.findFirstByExperimentId(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lever", "experiment:" + experimentId.toString()));

        // Capture details for audit before deletion
        UUID leverId = lever.getId();
        LeverType leverType = lever.getLeverType();
        UUID skuId = lever.getSku().getId();
        BigDecimal discountPercentage = lever.getLeverValue();

        leverRepository.delete(lever);

        log.info("Removed lever {} from experiment {}", leverId, experimentId);

        // Audit log
        logLeverRemoved(experimentId, leverId, leverType, skuId, discountPercentage);
    }

    /**
     * Gets the lever for an experiment if one exists.
     *
     * @param experimentId the experiment ID
     * @return the lever response, or null if no lever exists
     * @throws ResourceNotFoundException if experiment not found
     */
    @Transactional(readOnly = true)
    public LeverResponse getLever(UUID experimentId) {
        // Verify experiment exists
        if (!experimentRepository.existsById(experimentId)) {
            throw new ResourceNotFoundException("Experiment", experimentId.toString());
        }

        return leverRepository.findFirstByExperimentId(experimentId)
                .map(lever -> toLeverResponse(lever, experimentId))
                .orElse(null);
    }

    // --- Private helper methods ---

    private Experiment getExperimentOrThrow(UUID experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));
    }

    private Sku getSkuOrThrow(UUID skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Sku", skuId.toString()));
    }

    private void validateDraftStatus(Experiment experiment) {
        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new InvalidExperimentStateException(
                    experiment.getStatus(),
                    "modify lever",
                    ExperimentStatus.DRAFT
            );
        }
    }

    private void validateLeverType(LeverType type) {
        if (type != LeverType.PRICE_DISCOUNT) {
            throw new IllegalArgumentException(
                    "Invalid lever type: " + type + ". v0 only supports PRICE_DISCOUNT.");
        }
    }

    private void validateDiscountPercentage(BigDecimal discountPercentage) {
        if (discountPercentage == null) {
            throw new IllegalArgumentException("discountPercentage is required");
        }
        if (discountPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("discountPercentage must be greater than 0");
        }
        if (discountPercentage.compareTo(MAX_DISCOUNT_PERCENTAGE) > 0) {
            throw new IllegalArgumentException(
                    "discountPercentage must not exceed " + MAX_DISCOUNT_PERCENTAGE + "%");
        }
    }

    private void validateSkuInScope(UUID experimentId, UUID skuId) {
        if (!scopeRepository.existsByExperimentIdAndSkuId(experimentId, skuId)) {
            throw new IllegalArgumentException(
                    "SKU " + skuId + " is not in experiment scope. " +
                    "Add the SKU to experiment scope before configuring a lever for it.");
        }
    }

    private LeverResponse toLeverResponse(ExperimentLever lever, UUID experimentId) {
        return new LeverResponse(
                lever.getId(),
                experimentId,
                lever.getLeverType(),
                lever.getSku().getId(),
                lever.getSku().getSkuCode(),
                lever.getSku().getSkuName(),
                lever.getLeverValue()
        );
    }

    private void logLeverSet(UUID experimentId, ExperimentLever lever) {
        Map<String, Object> details = new HashMap<>();
        details.put("leverId", lever.getId().toString());
        details.put("type", lever.getLeverType().name());
        details.put("skuId", lever.getSku().getId().toString());
        details.put("discountPercentage", lever.getLeverValue());

        String detailsJson = serializeDetails(details);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_LEVER_SET,
                detailsJson
        );
    }

    private void logLeverRemoved(UUID experimentId, UUID leverId, LeverType type,
                                  UUID skuId, BigDecimal discountPercentage) {
        Map<String, Object> details = new HashMap<>();
        details.put("leverId", leverId.toString());
        details.put("type", type.name());
        details.put("skuId", skuId.toString());
        details.put("discountPercentage", discountPercentage);

        String detailsJson = serializeDetails(details);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_LEVER_REMOVED,
                detailsJson
        );
    }

    private String serializeDetails(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details", e);
            return "{}";
        }
    }
}
