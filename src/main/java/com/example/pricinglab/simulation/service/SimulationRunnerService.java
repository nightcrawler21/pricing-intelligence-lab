package com.example.pricinglab.simulation.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import com.example.pricinglab.experiment.repository.ExperimentLeverRepository;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.repository.ExperimentScopeRepository;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuCost;
import com.example.pricinglab.reference.sku.SkuCostRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.simulation.domain.SimulationResultDaily;
import com.example.pricinglab.simulation.domain.SimulationRun;
import com.example.pricinglab.simulation.dto.SimulationRunResponse;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository;
import com.example.pricinglab.simulation.repository.SimulationRunRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simulation Runner v0 - Deterministic, synchronous simulation engine.
 *
 * <p>This service executes pricing simulations for approved experiments.
 * It generates daily results for each store-SKU pair, comparing CONTROL
 * (baseline pricing) against TEST (with lever applied).</p>
 *
 * <h2>Key Characteristics:</h2>
 * <ul>
 *   <li><b>Synchronous</b>: All processing happens in a single transaction</li>
 *   <li><b>Deterministic</b>: Same inputs always produce same outputs</li>
 *   <li><b>Explainable</b>: Simple formulas that can be verified on paper</li>
 * </ul>
 *
 * <h2>Simulation Logic:</h2>
 *
 * <h3>CONTROL Variant:</h3>
 * <pre>
 * price = basePrice
 * unitsSold = baselineDailyUnits (fixed at 100 for v0)
 * revenue = price × unitsSold
 * cost = unitCost × unitsSold
 * margin = revenue - cost
 * </pre>
 *
 * <h3>TEST Variant (PRICE_DISCOUNT lever):</h3>
 * <pre>
 * testPrice = basePrice × (1 - discountPercent / 100)
 * priceChangePercent = (basePrice - testPrice) / basePrice
 * unitsMultiplier = 1 + (priceChangePercent × 1.5)
 * testUnits = round(baselineDailyUnits × unitsMultiplier)
 * revenue = testPrice × testUnits
 * cost = unitCost × testUnits
 * margin = revenue - cost
 * </pre>
 *
 * <p><b>Note:</b> v0 uses a fixed baseline of 100 units/day per store-SKU.
 * Future versions may load from historical sales data.</p>
 */
@Service
@Transactional
public class SimulationRunnerService {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunnerService.class);

    /**
     * v0 baseline daily units - fixed value for deterministic simulation.
     * Future versions may load from historical sales data.
     */
    private static final BigDecimal BASELINE_DAILY_UNITS = new BigDecimal("100");

    /**
     * Price elasticity multiplier for unit adjustment.
     * A 10% price reduction results in 15% more units sold.
     */
    private static final BigDecimal ELASTICITY_FACTOR = new BigDecimal("1.5");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ExperimentRepository experimentRepository;
    private final ExperimentScopeRepository scopeRepository;
    private final ExperimentLeverRepository leverRepository;
    private final BasePriceRepository basePriceRepository;
    private final SkuCostRepository skuCostRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final SimulationResultDailyRepository resultRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public SimulationRunnerService(
            ExperimentRepository experimentRepository,
            ExperimentScopeRepository scopeRepository,
            ExperimentLeverRepository leverRepository,
            BasePriceRepository basePriceRepository,
            SkuCostRepository skuCostRepository,
            SimulationRunRepository simulationRunRepository,
            SimulationResultDailyRepository resultRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.scopeRepository = scopeRepository;
        this.leverRepository = leverRepository;
        this.basePriceRepository = basePriceRepository;
        this.skuCostRepository = skuCostRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.resultRepository = resultRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs a complete simulation for an approved experiment.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Validates experiment is in APPROVED status</li>
     *   <li>Creates a SimulationRun record</li>
     *   <li>Generates daily results for each day × scope × variant</li>
     *   <li>Calculates summary metrics</li>
     *   <li>Marks run as COMPLETED</li>
     * </ol>
     *
     * <p>If any error occurs, the entire run is marked as FAILED.</p>
     *
     * @param experimentId the experiment to simulate
     * @return response containing run ID, status, and total days simulated
     * @throws ResourceNotFoundException if experiment not found
     * @throws InvalidExperimentStateException if experiment is not APPROVED
     * @throws IllegalStateException if required data is missing
     */
    public SimulationRunResponse runSimulation(UUID experimentId) {
        log.info("Starting simulation for experiment {}", experimentId);

        // 1. Load and validate experiment
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        validateApprovedStatus(experiment);

        // 2. Create simulation run
        SimulationRun run = new SimulationRun(UUID.randomUUID(), experiment);
        run.setStatus(SimulationStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = simulationRunRepository.save(run);

        try {
            // 3. Load required data
            List<ExperimentScope> scopes = scopeRepository.findByExperimentId(experimentId);
            if (scopes.isEmpty()) {
                throw new IllegalStateException("No scope entries found for experiment " + experimentId);
            }

            ExperimentLever lever = leverRepository.findFirstByExperimentId(experimentId)
                    .orElseThrow(() -> new IllegalStateException(
                            "No lever configured for experiment " + experimentId));

            if (lever.getLeverType() != LeverType.PRICE_DISCOUNT) {
                throw new IllegalStateException(
                        "v0 only supports PRICE_DISCOUNT lever type. Found: " + lever.getLeverType());
            }

            // 4. Calculate date range
            LocalDate startDate = experiment.getStartDate();
            LocalDate endDate = experiment.getEndDate();
            int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1; // inclusive
            run.setTotalDaysSimulated(totalDays);

            log.info("Simulating {} days ({} to {}) for {} scope entries",
                    totalDays, startDate, endDate, scopes.size());

            // 5. Generate daily results
            BigDecimal totalRevenueTest = BigDecimal.ZERO;
            BigDecimal totalRevenueControl = BigDecimal.ZERO;
            BigDecimal totalUnitsTest = BigDecimal.ZERO;
            BigDecimal totalUnitsControl = BigDecimal.ZERO;
            BigDecimal totalMarginTest = BigDecimal.ZERO;
            BigDecimal totalMarginControl = BigDecimal.ZERO;

            for (ExperimentScope scope : scopes) {
                Store store = scope.getStore();
                Sku sku = scope.getSku();

                // Get base price for this store-SKU
                BigDecimal basePrice = getBasePrice(sku.getId(), store.getId(), startDate);

                // Get unit cost for this SKU
                BigDecimal unitCost = getUnitCost(sku.getId(), startDate);

                // Calculate test price using lever
                BigDecimal discountPercent = lever.getLeverValue();
                BigDecimal testPrice = calculateTestPrice(basePrice, discountPercent);

                // Calculate units multiplier for test variant
                BigDecimal unitsMultiplier = calculateUnitsMultiplier(basePrice, testPrice);

                // Generate results for each day
                LocalDate currentDate = startDate;
                while (!currentDate.isAfter(endDate)) {
                    // CONTROL variant
                    SimulationResultDaily controlResult = createControlResult(
                            run, currentDate, store, sku, basePrice, unitCost);
                    run.addDailyResult(controlResult);

                    totalRevenueControl = totalRevenueControl.add(controlResult.getProjectedRevenue());
                    totalUnitsControl = totalUnitsControl.add(controlResult.getProjectedUnits());
                    totalMarginControl = totalMarginControl.add(controlResult.getProjectedMargin());

                    // TEST variant
                    SimulationResultDaily testResult = createTestResult(
                            run, currentDate, store, sku, basePrice, testPrice, unitCost, unitsMultiplier);
                    run.addDailyResult(testResult);

                    totalRevenueTest = totalRevenueTest.add(testResult.getProjectedRevenue());
                    totalUnitsTest = totalUnitsTest.add(testResult.getProjectedUnits());
                    totalMarginTest = totalMarginTest.add(testResult.getProjectedMargin());

                    currentDate = currentDate.plusDays(1);
                }
            }

            // 6. Set summary metrics
            run.setProjectedRevenueTest(totalRevenueTest);
            run.setProjectedRevenueControl(totalRevenueControl);
            run.setProjectedUnitsTest(totalUnitsTest);
            run.setProjectedUnitsControl(totalUnitsControl);
            run.setProjectedMarginTest(totalMarginTest);
            run.setProjectedMarginControl(totalMarginControl);

            // Calculate revenue lift percentage
            if (totalRevenueControl.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal revenueLift = totalRevenueTest.subtract(totalRevenueControl)
                        .divide(totalRevenueControl, 4, RoundingMode.HALF_UP)
                        .multiply(HUNDRED);
                run.setProjectedRevenueLiftPct(revenueLift);
            }

            // 7. Mark as completed
            run.setStatus(SimulationStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            run = simulationRunRepository.save(run);

            log.info("Simulation completed for experiment {}. Run ID: {}, Days: {}, Results: {}",
                    experimentId, run.getId(), totalDays, run.getDailyResults().size());

            // 8. Audit log
            logSimulationRun(experiment, run, lever);

            return toResponse(run);

        } catch (Exception e) {
            // Mark run as failed
            run.setStatus(SimulationStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setErrorMessage(e.getMessage());
            simulationRunRepository.save(run);

            log.error("Simulation failed for experiment {}: {}", experimentId, e.getMessage(), e);
            throw e;
        }
    }

    // --- Private helper methods ---

    private void validateApprovedStatus(Experiment experiment) {
        if (experiment.getStatus() != ExperimentStatus.APPROVED) {
            throw new InvalidExperimentStateException(
                    experiment.getStatus(),
                    "run simulation",
                    ExperimentStatus.APPROVED
            );
        }
    }

    private BigDecimal getBasePrice(UUID skuId, UUID storeId, LocalDate date) {
        return basePriceRepository.findEffectivePrice(skuId, storeId, date)
                .map(BasePrice::getPrice)
                .orElseThrow(() -> new IllegalStateException(
                        "No base price found for SKU " + skuId + " at store " + storeId +
                        " on date " + date + ". Base price data is required."));
    }

    private BigDecimal getUnitCost(UUID skuId, LocalDate date) {
        return skuCostRepository.findEffectiveCost(skuId, date)
                .map(SkuCost::getCost)
                .orElseThrow(() -> new IllegalStateException(
                        "No cost found for SKU " + skuId + " on date " + date +
                        ". Cost data is required for margin calculation."));
    }

    /**
     * Calculates test price by applying discount.
     * Formula: testPrice = basePrice × (1 - discountPercent / 100)
     */
    private BigDecimal calculateTestPrice(BigDecimal basePrice, BigDecimal discountPercent) {
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                discountPercent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        return basePrice.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates units multiplier based on price change.
     * Formula: unitsMultiplier = 1 + (priceChangePercent × 1.5)
     * where priceChangePercent = (basePrice - testPrice) / basePrice
     */
    private BigDecimal calculateUnitsMultiplier(BigDecimal basePrice, BigDecimal testPrice) {
        BigDecimal priceChange = basePrice.subtract(testPrice);
        BigDecimal priceChangePercent = priceChange.divide(basePrice, 4, RoundingMode.HALF_UP);
        return BigDecimal.ONE.add(priceChangePercent.multiply(ELASTICITY_FACTOR));
    }

    /**
     * Creates a CONTROL variant result for a single day.
     */
    private SimulationResultDaily createControlResult(
            SimulationRun run, LocalDate date, Store store, Sku sku,
            BigDecimal basePrice, BigDecimal unitCost) {

        SimulationResultDaily result = new SimulationResultDaily(
                UUID.randomUUID(), date, store, sku, false);

        result.setBasePrice(basePrice);
        result.setSimulatedPrice(basePrice); // CONTROL uses base price
        result.setProjectedUnits(BASELINE_DAILY_UNITS);
        result.setUnitCost(unitCost);

        BigDecimal revenue = basePrice.multiply(BASELINE_DAILY_UNITS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = unitCost.multiply(BASELINE_DAILY_UNITS).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margin = revenue.subtract(cost);

        result.setProjectedRevenue(revenue);
        result.setProjectedCost(cost);
        result.setProjectedMargin(margin);
        result.setBaselineUnits(BASELINE_DAILY_UNITS);
        result.setBaselineRevenue(revenue);

        return result;
    }

    /**
     * Creates a TEST variant result for a single day.
     */
    private SimulationResultDaily createTestResult(
            SimulationRun run, LocalDate date, Store store, Sku sku,
            BigDecimal basePrice, BigDecimal testPrice, BigDecimal unitCost,
            BigDecimal unitsMultiplier) {

        SimulationResultDaily result = new SimulationResultDaily(
                UUID.randomUUID(), date, store, sku, true);

        result.setBasePrice(basePrice);
        result.setSimulatedPrice(testPrice);

        // Calculate test units: baseline × multiplier, rounded to integer
        BigDecimal testUnits = BASELINE_DAILY_UNITS.multiply(unitsMultiplier)
                .setScale(0, RoundingMode.HALF_UP);
        result.setProjectedUnits(testUnits);
        result.setUnitCost(unitCost);

        BigDecimal revenue = testPrice.multiply(testUnits).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = unitCost.multiply(testUnits).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margin = revenue.subtract(cost);

        result.setProjectedRevenue(revenue);
        result.setProjectedCost(cost);
        result.setProjectedMargin(margin);
        result.setBaselineUnits(BASELINE_DAILY_UNITS);
        result.setBaselineRevenue(basePrice.multiply(BASELINE_DAILY_UNITS).setScale(2, RoundingMode.HALF_UP));

        return result;
    }

    private void logSimulationRun(Experiment experiment, SimulationRun run, ExperimentLever lever) {
        Map<String, Object> details = new HashMap<>();
        details.put("experimentId", experiment.getId().toString());
        details.put("simulationRunId", run.getId().toString());
        details.put("startDate", experiment.getStartDate().toString());
        details.put("endDate", experiment.getEndDate().toString());
        details.put("leverType", lever.getLeverType().name());
        details.put("leverValue", lever.getLeverValue());
        details.put("totalDaysSimulated", run.getTotalDaysSimulated());
        details.put("totalResultRows", run.getDailyResults().size());

        String detailsJson;
        try {
            detailsJson = objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details", e);
            detailsJson = "{}";
        }

        auditService.logAction(
                "Experiment",
                experiment.getId(),
                AuditAction.EXPERIMENT_SIMULATION_RUN,
                detailsJson
        );
    }

    private SimulationRunResponse toResponse(SimulationRun run) {
        return new SimulationRunResponse(
                run.getId(),
                run.getStatus(),
                run.getTotalDaysSimulated()
        );
    }
}
