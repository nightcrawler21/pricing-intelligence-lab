package com.example.pricinglab.experiment.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentGuardrails;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.dto.GuardrailsRequest;
import com.example.pricinglab.experiment.dto.GuardrailsResponse;
import com.example.pricinglab.experiment.repository.ExperimentGuardrailsRepository;
import com.example.pricinglab.experiment.repository.ExperimentLeverRepository;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing experiment guardrails.
 *
 * <h2>v0 Guardrails</h2>
 * <ul>
 *   <li>{@code priceFloor}: minimum allowed resulting price</li>
 *   <li>{@code priceCeiling}: maximum allowed resulting price</li>
 *   <li>{@code maxChangePercent}: maximum allowed absolute % change from base price</li>
 * </ul>
 *
 * <p>Guardrails are only editable when experiment is in DRAFT status.
 * Guardrails must exist and be valid before experiment can be submitted.</p>
 *
 * <h2>Validation Timing (Important)</h2>
 * <p>Guardrails are validated at <b>submit time</b> against <b>current effective base prices</b>
 * (i.e., prices effective as of {@code LocalDate.now()} at the moment of submission).
 * This means:</p>
 * <ul>
 *   <li>Validation does NOT use historical prices from before the experiment was created</li>
 *   <li>Validation does NOT use future-dated prices that may become effective during the experiment</li>
 *   <li>If base prices change between submit and simulation, the guardrail check at submit time
 *       may not reflect actual simulation prices</li>
 * </ul>
 * <p>This is intentional for v0 simplicity. Future versions may validate against the
 * experiment's date range or re-validate at simulation time.</p>
 */
@Service
@Transactional
public class ExperimentGuardrailsService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentGuardrailsService.class);

    private static final BigDecimal MAX_CHANGE_PERCENT_CAP = new BigDecimal("50.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ExperimentRepository experimentRepository;
    private final ExperimentGuardrailsRepository guardrailsRepository;
    private final ExperimentLeverRepository leverRepository;
    private final BasePriceRepository basePriceRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ExperimentGuardrailsService(
            ExperimentRepository experimentRepository,
            ExperimentGuardrailsRepository guardrailsRepository,
            ExperimentLeverRepository leverRepository,
            BasePriceRepository basePriceRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.guardrailsRepository = guardrailsRepository;
        this.leverRepository = leverRepository;
        this.basePriceRepository = basePriceRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Configures (creates or replaces) guardrails for an experiment.
     *
     * @param experimentId the experiment ID
     * @param request the guardrails configuration request
     * @return the configured guardrails
     * @throws ResourceNotFoundException if experiment not found
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     * @throws IllegalArgumentException if validation fails
     */
    public GuardrailsResponse configureGuardrails(UUID experimentId, GuardrailsRequest request) {
        log.info("Configuring guardrails for experiment {}: floor={}, ceiling={}, maxChange={}%",
                experimentId, request.priceFloor(), request.priceCeiling(), request.maxChangePercent());

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment);

        // Validate basic sanity rules
        validateSanityRules(request);

        // Check if lever exists and validate consistency
        Optional<ExperimentLever> lever = leverRepository.findFirstByExperimentId(experimentId);
        LeverValidationContext leverContext = null;

        if (lever.isPresent()) {
            leverContext = validateLeverConsistency(experiment, lever.get(), request);
        }

        // Create or update guardrails
        ExperimentGuardrails guardrails = guardrailsRepository.findByExperimentId(experimentId)
                .orElse(new ExperimentGuardrails(experimentId));

        guardrails.setExperiment(experiment);
        guardrails.setPriceFloor(request.priceFloor());
        guardrails.setPriceCeiling(request.priceCeiling());
        guardrails.setMaxChangePercent(request.maxChangePercent());

        guardrails = guardrailsRepository.save(guardrails);

        log.info("Configured guardrails for experiment {}", experimentId);

        // Audit log
        logGuardrailsSet(experimentId, guardrails, lever.orElse(null), leverContext);

        return toGuardrailsResponse(guardrails, experimentId);
    }

    /**
     * Removes guardrails from an experiment.
     *
     * @param experimentId the experiment ID
     * @throws ResourceNotFoundException if experiment not found or no guardrails exist
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public void removeGuardrails(UUID experimentId) {
        log.info("Removing guardrails from experiment {}", experimentId);

        // Load experiment and validate status
        Experiment experiment = getExperimentOrThrow(experimentId);
        validateDraftStatus(experiment);

        // Find existing guardrails
        ExperimentGuardrails guardrails = guardrailsRepository.findByExperimentId(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Guardrails", "experiment:" + experimentId.toString()));

        // Capture details for audit before deletion
        BigDecimal priceFloor = guardrails.getPriceFloor();
        BigDecimal priceCeiling = guardrails.getPriceCeiling();
        BigDecimal maxChangePercent = guardrails.getMaxChangePercent();

        guardrailsRepository.delete(guardrails);

        log.info("Removed guardrails from experiment {}", experimentId);

        // Audit log
        logGuardrailsRemoved(experimentId, priceFloor, priceCeiling, maxChangePercent);
    }

    /**
     * Validates that guardrails exist and are valid for submission.
     * Called during experiment submit flow.
     *
     * @param experiment the experiment to validate
     * @throws IllegalArgumentException if guardrails are missing or invalid
     */
    public void validateForSubmit(Experiment experiment) {
        UUID experimentId = experiment.getId();

        // Check guardrails exist
        ExperimentGuardrails guardrails = guardrailsRepository.findByExperimentId(experimentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot submit experiment without guardrails configured."));

        // Validate v0 fields are set
        if (guardrails.getPriceFloor() == null || guardrails.getPriceCeiling() == null
                || guardrails.getMaxChangePercent() == null) {
            throw new IllegalArgumentException(
                    "Cannot submit experiment: guardrails are incomplete. " +
                    "Please configure priceFloor, priceCeiling, and maxChangePercent.");
        }

        // Validate sanity rules
        GuardrailsRequest request = new GuardrailsRequest(
                guardrails.getPriceFloor(),
                guardrails.getPriceCeiling(),
                guardrails.getMaxChangePercent()
        );
        validateSanityRules(request);

        // Validate lever consistency if lever exists
        Optional<ExperimentLever> lever = leverRepository.findFirstByExperimentId(experimentId);
        if (lever.isPresent()) {
            validateLeverConsistency(experiment, lever.get(), request);
        }

        log.info("Guardrails validated successfully for experiment {}", experimentId);
    }

    // --- Private helper methods ---

    private Experiment getExperimentOrThrow(UUID experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));
    }

    private void validateDraftStatus(Experiment experiment) {
        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new InvalidExperimentStateException(
                    experiment.getStatus(),
                    "modify guardrails",
                    ExperimentStatus.DRAFT
            );
        }
    }

    private void validateSanityRules(GuardrailsRequest request) {
        // priceFloor > 0 (handled by annotation, but double-check)
        if (request.priceFloor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("priceFloor must be greater than 0");
        }

        // priceCeiling > 0 (handled by annotation, but double-check)
        if (request.priceCeiling().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("priceCeiling must be greater than 0");
        }

        // priceFloor < priceCeiling
        if (request.priceFloor().compareTo(request.priceCeiling()) >= 0) {
            throw new IllegalArgumentException(
                    "priceFloor (" + request.priceFloor() + ") must be less than priceCeiling (" +
                    request.priceCeiling() + ")");
        }

        // maxChangePercent > 0 and <= 50 (handled by annotation, but double-check)
        if (request.maxChangePercent().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxChangePercent must be greater than 0");
        }
        if (request.maxChangePercent().compareTo(MAX_CHANGE_PERCENT_CAP) > 0) {
            throw new IllegalArgumentException(
                    "maxChangePercent must not exceed " + MAX_CHANGE_PERCENT_CAP + "%");
        }
    }

    /**
     * Validates lever consistency with guardrails.
     *
     * For PRICE_DISCOUNT lever:
     * - leverPrice = basePrice * (1 - discountPercent/100)
     * - leverPrice >= priceFloor
     * - leverPrice <= priceCeiling
     * - abs(leverPrice - basePrice) / basePrice * 100 <= maxChangePercent
     *
     * Base price lookup (v0):
     * Uses the MINIMUM base price across all stores for the lever SKU (most conservative).
     * This ensures guardrails are safe across all possible store-level prices.
     *
     * @return LeverValidationContext with computed values for audit logging
     */
    private LeverValidationContext validateLeverConsistency(
            Experiment experiment, ExperimentLever lever, GuardrailsRequest request) {

        if (lever.getLeverType() != LeverType.PRICE_DISCOUNT) {
            // v0 only validates PRICE_DISCOUNT
            log.warn("Lever type {} not validated in v0 guardrail check", lever.getLeverType());
            return null;
        }

        UUID skuId = lever.getSku().getId();
        BigDecimal discountPercent = lever.getLeverValue();

        // Find minimum base price for the SKU (most conservative approach)
        // v0 decision: Use minimum base price across ALL stores with effective prices today.
        // This is conservative because it validates against the lowest price scenario.
        LocalDate today = LocalDate.now();
        List<BasePrice> basePrices = basePriceRepository.findAllEffectivePricesForSku(skuId, today);

        if (basePrices.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot validate guardrails: no base price found for SKU " + skuId + ". " +
                    "Base price data is required for guardrail validation.");
        }

        // Find minimum base price (most conservative)
        BigDecimal basePrice = basePrices.stream()
                .map(BasePrice::getPrice)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        // Calculate lever-implied price: basePrice * (1 - discountPercent/100)
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                discountPercent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        BigDecimal leverPrice = basePrice.multiply(discountMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        // Validate: leverPrice >= priceFloor
        if (leverPrice.compareTo(request.priceFloor()) < 0) {
            throw new IllegalArgumentException(
                    "Lever-implied price (" + leverPrice + " THB) is below priceFloor (" +
                    request.priceFloor() + " THB). Computed from base price " + basePrice +
                    " THB with " + discountPercent + "% discount.");
        }

        // Validate: leverPrice <= priceCeiling
        if (leverPrice.compareTo(request.priceCeiling()) > 0) {
            throw new IllegalArgumentException(
                    "Lever-implied price (" + leverPrice + " THB) exceeds priceCeiling (" +
                    request.priceCeiling() + " THB). Computed from base price " + basePrice +
                    " THB with " + discountPercent + "% discount.");
        }

        // Validate: abs(leverPrice - basePrice) / basePrice * 100 <= maxChangePercent
        BigDecimal priceChange = leverPrice.subtract(basePrice).abs();
        BigDecimal changePercent = priceChange.divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        if (changePercent.compareTo(request.maxChangePercent()) > 0) {
            throw new IllegalArgumentException(
                    "Price change (" + changePercent + "%) exceeds maxChangePercent (" +
                    request.maxChangePercent() + "%). Lever-implied price is " + leverPrice +
                    " THB vs base price " + basePrice + " THB.");
        }

        log.info("Lever consistency validated: basePrice={}, leverPrice={}, changePercent={}%",
                basePrice, leverPrice, changePercent);

        return new LeverValidationContext(basePrice, leverPrice, changePercent);
    }

    private GuardrailsResponse toGuardrailsResponse(ExperimentGuardrails guardrails, UUID experimentId) {
        return new GuardrailsResponse(
                experimentId,
                guardrails.getPriceFloor(),
                guardrails.getPriceCeiling(),
                guardrails.getMaxChangePercent()
        );
    }

    private void logGuardrailsSet(UUID experimentId, ExperimentGuardrails guardrails,
                                   ExperimentLever lever, LeverValidationContext leverContext) {
        Map<String, Object> details = new HashMap<>();
        details.put("priceFloor", guardrails.getPriceFloor());
        details.put("priceCeiling", guardrails.getPriceCeiling());
        details.put("maxChangePercent", guardrails.getMaxChangePercent());

        // Include lever details if present (for audit explainability)
        if (lever != null && leverContext != null) {
            details.put("leverType", lever.getLeverType().name());
            details.put("leverSkuId", lever.getSku().getId().toString());
            details.put("leverDiscountPercent", lever.getLeverValue());
            details.put("computedBasePrice", leverContext.basePrice);
            details.put("computedLeverPrice", leverContext.leverPrice);
        }

        String detailsJson = serializeDetails(details);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_GUARDRAILS_SET,
                detailsJson
        );
    }

    private void logGuardrailsRemoved(UUID experimentId, BigDecimal priceFloor,
                                       BigDecimal priceCeiling, BigDecimal maxChangePercent) {
        Map<String, Object> details = new HashMap<>();
        details.put("priceFloor", priceFloor);
        details.put("priceCeiling", priceCeiling);
        details.put("maxChangePercent", maxChangePercent);

        String detailsJson = serializeDetails(details);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_GUARDRAILS_REMOVED,
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

    /**
     * Context holder for lever validation results (for audit logging).
     */
    private record LeverValidationContext(
            BigDecimal basePrice,
            BigDecimal leverPrice,
            BigDecimal changePercent
    ) {}
}
