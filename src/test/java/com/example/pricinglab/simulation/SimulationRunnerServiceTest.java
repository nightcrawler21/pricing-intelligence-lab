package com.example.pricinglab.simulation;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
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
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
import com.example.pricinglab.simulation.domain.SimulationResultDaily;
import com.example.pricinglab.simulation.domain.SimulationRun;
import com.example.pricinglab.simulation.dto.SimulationRunResponse;
import com.example.pricinglab.simulation.repository.SimulationResultDailyRepository;
import com.example.pricinglab.simulation.repository.SimulationRunRepository;
import com.example.pricinglab.simulation.service.SimulationRunnerService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SimulationRunnerService (Story 2.6).
 *
 * Tests verify:
 * 1. Simulation runs successfully for a valid APPROVED experiment
 * 2. CONTROL vs TEST prices differ correctly
 * 3. UnitsSold changes deterministically
 * 4. Total result rows = days × scope × 2 variants
 * 5. Simulation fails if experiment is not APPROVED
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimulationRunnerServiceTest {

    @Autowired
    private SimulationRunnerService simulationRunnerService;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentScopeRepository scopeRepository;

    @Autowired
    private ExperimentLeverRepository leverRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private BasePriceRepository basePriceRepository;

    @Autowired
    private SkuCostRepository skuCostRepository;

    @Autowired
    private SimulationRunRepository simulationRunRepository;

    @Autowired
    private SimulationResultDailyRepository resultRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private Sku sku;
    private Experiment experiment;

    // Test constants
    private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal UNIT_COST = new BigDecimal("60.00");
    private static final BigDecimal DISCOUNT_PERCENT = new BigDecimal("10.0"); // 10% discount
    private static final BigDecimal BASELINE_UNITS = new BigDecimal("100");
    private static final BigDecimal ELASTICITY_FACTOR = new BigDecimal("1.5");

    @BeforeEach
    void setUp() {
        // Create test store
        store = new Store(UUID.randomUUID(), "SIM001", "Simulation Test Store");
        store.setRegion("Bangkok");
        store.setFormat("Hypermarket");
        store = storeRepository.save(store);

        // Create test SKU
        sku = new Sku(UUID.randomUUID(), "SIM-SKU-001", "Simulation Test Product");
        sku.setCategory("Beverages");
        sku.setBrand("Test Brand");
        sku = skuRepository.save(sku);

        // Create base price - effective from 2024-01-01 to cover all test dates
        BasePrice basePrice = new BasePrice(
                UUID.randomUUID(), sku, store, BASE_PRICE,
                LocalDate.of(2024, 1, 1));
        basePriceRepository.save(basePrice);

        // Create SKU cost - effective from 2024-01-01 to cover all test dates
        SkuCost skuCost = new SkuCost(
                UUID.randomUUID(), sku, UNIT_COST,
                LocalDate.of(2024, 1, 1));
        skuCostRepository.save(skuCost);

        entityManager.flush();
    }

    private Experiment createApprovedExperiment(LocalDate startDate, LocalDate endDate) {
        Experiment exp = new Experiment(
                UUID.randomUUID(),
                "Simulation Test Experiment",
                startDate,
                endDate
        );
        exp.setStatus(ExperimentStatus.APPROVED);
        exp.setDescription("Test experiment for simulation");
        exp = experimentRepository.save(exp);

        // Add scope
        ExperimentScope scope = new ExperimentScope(UUID.randomUUID(), store, sku, true);
        scope.setExperiment(exp);
        scopeRepository.save(scope);

        // Add lever (10% discount)
        ExperimentLever lever = new ExperimentLever(
                UUID.randomUUID(), sku, LeverType.PRICE_DISCOUNT, DISCOUNT_PERCENT);
        lever.setExperiment(exp);
        leverRepository.save(lever);

        entityManager.flush();
        return exp;
    }

    @Nested
    @DisplayName("Successful Simulation")
    class SuccessfulSimulation {

        @Test
        @DisplayName("Simulation runs successfully for APPROVED experiment")
        void simulationRunsSuccessfully() {
            // Given: APPROVED experiment with 7-day duration
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 7);
            experiment = createApprovedExperiment(startDate, endDate);

            // When: Run simulation
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then: Simulation completes successfully
            assertThat(response.status()).isEqualTo(SimulationStatus.COMPLETED);
            assertThat(response.simulationRunId()).isNotNull();
            assertThat(response.totalDaysSimulated()).isEqualTo(7);

            // Verify SimulationRun was persisted
            SimulationRun run = simulationRunRepository.findById(response.simulationRunId()).orElseThrow();
            assertThat(run.getStatus()).isEqualTo(SimulationStatus.COMPLETED);
            assertThat(run.getStartedAt()).isNotNull();
            assertThat(run.getCompletedAt()).isNotNull();
            assertThat(run.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Total result rows = days × scope entries × 2 variants")
        void correctNumberOfResultRows() {
            // Given: 5-day experiment with 1 store-SKU scope
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 5);
            experiment = createApprovedExperiment(startDate, endDate);

            // When: Run simulation
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then: Total rows = 5 days × 1 scope × 2 variants = 10
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            assertThat(results).hasSize(10);

            // Verify 5 CONTROL + 5 TEST
            long controlCount = results.stream().filter(r -> !r.isTestGroup()).count();
            long testCount = results.stream().filter(SimulationResultDaily::isTestGroup).count();
            assertThat(controlCount).isEqualTo(5);
            assertThat(testCount).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Price Calculations")
    class PriceCalculations {

        @Test
        @DisplayName("CONTROL price equals base price")
        void controlPriceEqualsBasePrice() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1); // Single day
            experiment = createApprovedExperiment(startDate, endDate);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            SimulationResultDaily controlResult = results.stream()
                    .filter(r -> !r.isTestGroup())
                    .findFirst()
                    .orElseThrow();

            assertThat(controlResult.getBasePrice()).isEqualByComparingTo(BASE_PRICE);
            assertThat(controlResult.getSimulatedPrice()).isEqualByComparingTo(BASE_PRICE);
        }

        @Test
        @DisplayName("TEST price reflects discount correctly")
        void testPriceReflectsDiscount() {
            // Given: 10% discount
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            experiment = createApprovedExperiment(startDate, endDate);

            // Expected: testPrice = 100.00 × (1 - 10/100) = 90.00
            BigDecimal expectedTestPrice = BASE_PRICE.multiply(
                    BigDecimal.ONE.subtract(DISCOUNT_PERCENT.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            SimulationResultDaily testResult = results.stream()
                    .filter(SimulationResultDaily::isTestGroup)
                    .findFirst()
                    .orElseThrow();

            assertThat(testResult.getBasePrice()).isEqualByComparingTo(BASE_PRICE);
            assertThat(testResult.getSimulatedPrice()).isEqualByComparingTo(expectedTestPrice);
            assertThat(testResult.getSimulatedPrice()).isEqualByComparingTo(new BigDecimal("90.00"));
        }
    }

    @Nested
    @DisplayName("Units Sold Calculations")
    class UnitsSoldCalculations {

        @Test
        @DisplayName("CONTROL units equals baseline (100)")
        void controlUnitsEqualsBaseline() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            experiment = createApprovedExperiment(startDate, endDate);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            SimulationResultDaily controlResult = results.stream()
                    .filter(r -> !r.isTestGroup())
                    .findFirst()
                    .orElseThrow();

            assertThat(controlResult.getProjectedUnits()).isEqualByComparingTo(BASELINE_UNITS);
        }

        @Test
        @DisplayName("TEST units increase deterministically based on price elasticity")
        void testUnitsIncreaseDeterministically() {
            // Given: 10% discount with elasticity factor 1.5
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            experiment = createApprovedExperiment(startDate, endDate);

            // Calculate expected:
            // testPrice = 100 × 0.9 = 90
            // priceChangePercent = (100 - 90) / 100 = 0.1
            // unitsMultiplier = 1 + (0.1 × 1.5) = 1.15
            // testUnits = 100 × 1.15 = 115
            BigDecimal testPrice = new BigDecimal("90.00");
            BigDecimal priceChangePercent = BASE_PRICE.subtract(testPrice)
                    .divide(BASE_PRICE, 4, RoundingMode.HALF_UP);
            BigDecimal unitsMultiplier = BigDecimal.ONE.add(priceChangePercent.multiply(ELASTICITY_FACTOR));
            BigDecimal expectedUnits = BASELINE_UNITS.multiply(unitsMultiplier)
                    .setScale(0, RoundingMode.HALF_UP);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            SimulationResultDaily testResult = results.stream()
                    .filter(SimulationResultDaily::isTestGroup)
                    .findFirst()
                    .orElseThrow();

            assertThat(testResult.getProjectedUnits()).isEqualByComparingTo(expectedUnits);
            assertThat(testResult.getProjectedUnits()).isEqualByComparingTo(new BigDecimal("115"));
        }

        @Test
        @DisplayName("Simulation is deterministic - same inputs produce same outputs")
        void simulationIsDeterministic() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 3);
            experiment = createApprovedExperiment(startDate, endDate);

            // When: Run simulation twice
            SimulationRunResponse response1 = simulationRunnerService.runSimulation(experiment.getId());

            // Need to reset experiment status for second run
            experiment.setStatus(ExperimentStatus.APPROVED);
            experimentRepository.save(experiment);
            entityManager.flush();

            SimulationRunResponse response2 = simulationRunnerService.runSimulation(experiment.getId());

            // Then: Results should be identical (same values, different IDs)
            List<SimulationResultDaily> results1 = resultRepository.findBySimulationRunId(response1.simulationRunId());
            List<SimulationResultDaily> results2 = resultRepository.findBySimulationRunId(response2.simulationRunId());

            assertThat(results1).hasSameSizeAs(results2);

            // Compare sorted by date and testGroup
            results1.sort((a, b) -> {
                int dateCompare = a.getSimulationDate().compareTo(b.getSimulationDate());
                if (dateCompare != 0) return dateCompare;
                return Boolean.compare(a.isTestGroup(), b.isTestGroup());
            });
            results2.sort((a, b) -> {
                int dateCompare = a.getSimulationDate().compareTo(b.getSimulationDate());
                if (dateCompare != 0) return dateCompare;
                return Boolean.compare(a.isTestGroup(), b.isTestGroup());
            });

            for (int i = 0; i < results1.size(); i++) {
                SimulationResultDaily r1 = results1.get(i);
                SimulationResultDaily r2 = results2.get(i);
                assertThat(r1.getSimulatedPrice()).isEqualByComparingTo(r2.getSimulatedPrice());
                assertThat(r1.getProjectedUnits()).isEqualByComparingTo(r2.getProjectedUnits());
                assertThat(r1.getProjectedRevenue()).isEqualByComparingTo(r2.getProjectedRevenue());
                assertThat(r1.getProjectedMargin()).isEqualByComparingTo(r2.getProjectedMargin());
            }
        }
    }

    @Nested
    @DisplayName("Revenue and Margin Calculations")
    class RevenueAndMarginCalculations {

        @Test
        @DisplayName("CONTROL revenue = price × units")
        void controlRevenueCalculation() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            experiment = createApprovedExperiment(startDate, endDate);

            // Expected: 100.00 × 100 = 10000.00
            BigDecimal expectedRevenue = BASE_PRICE.multiply(BASELINE_UNITS);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());
            SimulationResultDaily controlResult = results.stream()
                    .filter(r -> !r.isTestGroup())
                    .findFirst()
                    .orElseThrow();

            assertThat(controlResult.getProjectedRevenue()).isEqualByComparingTo(expectedRevenue);
        }

        @Test
        @DisplayName("Margin = revenue - cost")
        void marginCalculation() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            experiment = createApprovedExperiment(startDate, endDate);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            List<SimulationResultDaily> results = resultRepository.findBySimulationRunId(response.simulationRunId());

            for (SimulationResultDaily result : results) {
                BigDecimal expectedMargin = result.getProjectedRevenue().subtract(result.getProjectedCost());
                assertThat(result.getProjectedMargin()).isEqualByComparingTo(expectedMargin);
            }
        }
    }

    @Nested
    @DisplayName("Summary Metrics")
    class SummaryMetrics {

        @Test
        @DisplayName("Summary metrics are calculated correctly")
        void summaryMetricsCalculation() {
            // Given: 3-day experiment
            LocalDate startDate = LocalDate.of(2025, 1, 1);
            LocalDate endDate = LocalDate.of(2025, 1, 3);
            experiment = createApprovedExperiment(startDate, endDate);

            // When
            SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());

            // Then
            SimulationRun run = simulationRunRepository.findById(response.simulationRunId()).orElseThrow();

            // Verify summary metrics are populated
            assertThat(run.getProjectedRevenueTest()).isNotNull();
            assertThat(run.getProjectedRevenueControl()).isNotNull();
            assertThat(run.getProjectedUnitsTest()).isNotNull();
            assertThat(run.getProjectedUnitsControl()).isNotNull();
            assertThat(run.getProjectedMarginTest()).isNotNull();
            assertThat(run.getProjectedMarginControl()).isNotNull();
            assertThat(run.getProjectedRevenueLiftPct()).isNotNull();

            // TEST should have lower revenue (discounted price) but higher units
            assertThat(run.getProjectedUnitsTest())
                    .isGreaterThan(run.getProjectedUnitsControl());

            // Revenue lift should be negative (discount reduces revenue)
            // Actually with 10% discount and 15% more units, revenue change is:
            // TEST: 90 × 115 × 3 = 31050
            // CONTROL: 100 × 100 × 3 = 30000
            // Lift = (31050 - 30000) / 30000 = 3.5%
            assertThat(run.getProjectedRevenueLiftPct()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Failure Scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("Simulation fails if experiment is not APPROVED")
        void failsIfNotApproved() {
            // Given: DRAFT experiment
            Experiment draftExperiment = new Experiment(
                    UUID.randomUUID(),
                    "Draft Experiment",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 7)
            );
            draftExperiment.setStatus(ExperimentStatus.DRAFT);
            draftExperiment = experimentRepository.save(draftExperiment);
            entityManager.flush();

            final UUID experimentId = draftExperiment.getId();

            // When/Then
            assertThatThrownBy(() -> simulationRunnerService.runSimulation(experimentId))
                    .isInstanceOf(InvalidExperimentStateException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("APPROVED");
        }

        @Test
        @DisplayName("Simulation fails if experiment is PENDING_APPROVAL")
        void failsIfPendingApproval() {
            // Given: PENDING_APPROVAL experiment
            Experiment pendingExperiment = new Experiment(
                    UUID.randomUUID(),
                    "Pending Experiment",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 7)
            );
            pendingExperiment.setStatus(ExperimentStatus.PENDING_APPROVAL);
            pendingExperiment = experimentRepository.save(pendingExperiment);
            entityManager.flush();

            final UUID experimentId = pendingExperiment.getId();

            // When/Then
            assertThatThrownBy(() -> simulationRunnerService.runSimulation(experimentId))
                    .isInstanceOf(InvalidExperimentStateException.class)
                    .hasMessageContaining("PENDING_APPROVAL");
        }

        @Test
        @DisplayName("Simulation fails if no scope entries")
        void failsIfNoScope() {
            // Given: APPROVED experiment without scope
            Experiment exp = new Experiment(
                    UUID.randomUUID(),
                    "No Scope Experiment",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 7)
            );
            exp.setStatus(ExperimentStatus.APPROVED);
            exp = experimentRepository.save(exp);
            entityManager.flush();

            final UUID experimentId = exp.getId();

            // When/Then
            assertThatThrownBy(() -> simulationRunnerService.runSimulation(experimentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No scope entries found");
        }

        @Test
        @DisplayName("Simulation fails if no lever configured")
        void failsIfNoLever() {
            // Given: APPROVED experiment with scope but no lever
            Experiment exp = new Experiment(
                    UUID.randomUUID(),
                    "No Lever Experiment",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 7)
            );
            exp.setStatus(ExperimentStatus.APPROVED);
            exp = experimentRepository.save(exp);

            // Add scope only, no lever
            ExperimentScope scope = new ExperimentScope(UUID.randomUUID(), store, sku, true);
            scope.setExperiment(exp);
            scopeRepository.save(scope);
            entityManager.flush();

            final UUID experimentId = exp.getId();

            // When/Then
            assertThatThrownBy(() -> simulationRunnerService.runSimulation(experimentId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No lever configured");
        }
    }
}
