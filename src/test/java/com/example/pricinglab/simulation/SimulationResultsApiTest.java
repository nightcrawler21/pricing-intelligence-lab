package com.example.pricinglab.simulation;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.enums.SimulationStatus;
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
import com.example.pricinglab.simulation.dto.SimulationBreakdownRowResponse;
import com.example.pricinglab.simulation.dto.SimulationRunListItemResponse;
import com.example.pricinglab.simulation.dto.SimulationRunStatusResponse;
import com.example.pricinglab.simulation.dto.SimulationSummaryResponse;
import com.example.pricinglab.simulation.dto.SimulationTimeseriesPointResponse;
import com.example.pricinglab.simulation.dto.SimulationRunResponse;
import com.example.pricinglab.simulation.service.SimulationResultsQueryService;
import com.example.pricinglab.simulation.service.SimulationRunnerService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Simulation Results API (Story 2.7).
 *
 * Tests verify all read-only endpoints for viewing simulation data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SimulationResultsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SimulationRunnerService simulationRunnerService;

    @Autowired
    private SimulationResultsQueryService queryService;

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
    private EntityManager entityManager;

    private Store store;
    private Sku sku;
    private Experiment experiment;
    private UUID simulationRunId;

    private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal UNIT_COST = new BigDecimal("60.00");
    private static final BigDecimal DISCOUNT_PERCENT = new BigDecimal("10.0");
    private static final LocalDate START_DATE = LocalDate.of(2025, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 1, 5); // 5 days

    @BeforeEach
    void setUp() {
        // Create test store
        store = new Store(UUID.randomUUID(), "RES001", "Results Test Store");
        store.setRegion("Bangkok");
        store.setFormat("Hypermarket");
        store = storeRepository.save(store);

        // Create test SKU
        sku = new Sku(UUID.randomUUID(), "RES-SKU-001", "Results Test Product");
        sku.setCategory("Beverages");
        sku.setBrand("Test Brand");
        sku = skuRepository.save(sku);

        // Create base price
        BasePrice basePrice = new BasePrice(
                UUID.randomUUID(), sku, store, BASE_PRICE,
                LocalDate.of(2024, 1, 1));
        basePriceRepository.save(basePrice);

        // Create SKU cost
        SkuCost skuCost = new SkuCost(
                UUID.randomUUID(), sku, UNIT_COST,
                LocalDate.of(2024, 1, 1));
        skuCostRepository.save(skuCost);

        // Create and approve experiment
        experiment = createApprovedExperiment();

        // Run simulation
        SimulationRunResponse response = simulationRunnerService.runSimulation(experiment.getId());
        simulationRunId = response.simulationRunId();

        entityManager.flush();
        entityManager.clear();
    }

    private Experiment createApprovedExperiment() {
        Experiment exp = new Experiment(
                UUID.randomUUID(),
                "Results Test Experiment",
                START_DATE,
                END_DATE
        );
        exp.setStatus(ExperimentStatus.APPROVED);
        exp.setDescription("Test experiment for simulation results API");
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
    @DisplayName("GET /api/experiments/{experimentId}/simulation-runs")
    class ListRunsEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns list of runs for experiment, most recent first")
        void listRunsForExperiment() throws Exception {
            mockMvc.perform(get("/api/experiments/{experimentId}/simulation-runs", experiment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].runId").value(simulationRunId.toString()))
                    .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$[0].totalDaysSimulated").value(5))
                    .andExpect(jsonPath("$[0].startedAt").exists())
                    .andExpect(jsonPath("$[0].completedAt").exists())
                    .andExpect(jsonPath("$[0].errorMessage").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns empty list for experiment with no runs")
        void returnsEmptyListForNoRuns() throws Exception {
            // Create another experiment without running simulation
            Experiment noRunExperiment = new Experiment(
                    UUID.randomUUID(), "No Run Experiment",
                    START_DATE, END_DATE);
            noRunExperiment.setStatus(ExperimentStatus.APPROVED);
            noRunExperiment = experimentRepository.save(noRunExperiment);
            entityManager.flush();

            mockMvc.perform(get("/api/experiments/{experimentId}/simulation-runs", noRunExperiment.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Respects limit parameter")
        void respectsLimitParameter() throws Exception {
            mockMvc.perform(get("/api/experiments/{experimentId}/simulation-runs", experiment.getId())
                            .param("limit", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/simulation-runs/{runId}/status")
    class StatusEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns run status")
        void returnsRunStatus() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/status", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runId").value(simulationRunId.toString()))
                    .andExpect(jsonPath("$.experimentId").value(experiment.getId().toString()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.startedAt").exists())
                    .andExpect(jsonPath("$.completedAt").exists())
                    .andExpect(jsonPath("$.errorMessage").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns 404 for unknown run ID")
        void returns404ForUnknownRunId() throws Exception {
            UUID unknownId = UUID.randomUUID();
            mockMvc.perform(get("/api/simulation-runs/{runId}/status", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.detail").value(containsString("SimulationRun")));
        }
    }

    @Nested
    @DisplayName("GET /api/simulation-runs/{runId}/summary")
    class SummaryEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns summary metrics with control/test/delta")
        void returnsSummaryMetrics() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/summary", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.runId").value(simulationRunId.toString()))
                    .andExpect(jsonPath("$.control.units").exists())
                    .andExpect(jsonPath("$.control.revenue").exists())
                    .andExpect(jsonPath("$.control.margin").exists())
                    .andExpect(jsonPath("$.test.units").exists())
                    .andExpect(jsonPath("$.test.revenue").exists())
                    .andExpect(jsonPath("$.test.margin").exists())
                    .andExpect(jsonPath("$.delta.units").exists())
                    .andExpect(jsonPath("$.delta.revenue").exists())
                    .andExpect(jsonPath("$.delta.margin").exists())
                    .andExpect(jsonPath("$.delta.revenuePct").exists())
                    .andExpect(jsonPath("$.delta.marginPct").exists());
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Summary totals match deterministic expectations")
        void summaryTotalsMatchExpectations() throws Exception {
            // Expected values for 5 days, 1 scope:
            // CONTROL: 100 units/day × 5 days = 500 units
            // CONTROL: 100.00 price × 500 = 50000 revenue
            // TEST: 115 units/day × 5 days = 575 units
            // TEST: 90.00 price × 575 = 51750 revenue
            // Delta: 575 - 500 = 75 units, 51750 - 50000 = 1750 revenue

            mockMvc.perform(get("/api/simulation-runs/{runId}/summary", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.control.units").value(500))
                    .andExpect(jsonPath("$.test.units").value(575))
                    .andExpect(jsonPath("$.delta.units").value(75));
        }
    }

    @Nested
    @DisplayName("GET /api/simulation-runs/{runId}/timeseries")
    class TimeseriesEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns timeseries data sorted by date")
        void returnsTimeseriesSortedByDate() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/timeseries", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5))) // 5 days
                    .andExpect(jsonPath("$[0].date").value("2025-01-01"))
                    .andExpect(jsonPath("$[1].date").value("2025-01-02"))
                    .andExpect(jsonPath("$[2].date").value("2025-01-03"))
                    .andExpect(jsonPath("$[3].date").value("2025-01-04"))
                    .andExpect(jsonPath("$[4].date").value("2025-01-05"));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Timeseries contains control/test/delta for each day")
        void timeseriesContainsAllMetrics() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/timeseries", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].control.units").value(100))
                    .andExpect(jsonPath("$[0].control.revenue").value(10000.00))
                    .andExpect(jsonPath("$[0].test.units").value(115))
                    .andExpect(jsonPath("$[0].test.revenue").value(10350.00))
                    .andExpect(jsonPath("$[0].delta.units").value(15));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Accepts groupBy=DATE parameter")
        void acceptsGroupByDate() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/timeseries", simulationRunId)
                            .param("groupBy", "DATE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Rejects invalid groupBy parameter")
        void rejectsInvalidGroupBy() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/timeseries", simulationRunId)
                            .param("groupBy", "WEEK"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("groupBy")))
                    .andExpect(jsonPath("$.detail").value(containsString("DATE")));
        }
    }

    @Nested
    @DisplayName("GET /api/simulation-runs/{runId}/breakdown")
    class BreakdownEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Breakdown by STORE works")
        void breakdownByStore() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", simulationRunId)
                            .param("by", "STORE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1))) // 1 store
                    .andExpect(jsonPath("$[0].storeId").value(store.getId().toString()))
                    .andExpect(jsonPath("$[0].skuId").doesNotExist())
                    .andExpect(jsonPath("$[0].date").doesNotExist())
                    .andExpect(jsonPath("$[0].control.units").value(500))
                    .andExpect(jsonPath("$[0].test.units").value(575));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Breakdown by SKU works")
        void breakdownBySku() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", simulationRunId)
                            .param("by", "SKU"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1))) // 1 SKU
                    .andExpect(jsonPath("$[0].skuId").value(sku.getId().toString()))
                    .andExpect(jsonPath("$[0].storeId").doesNotExist())
                    .andExpect(jsonPath("$[0].date").doesNotExist());
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Breakdown by DATE works and is sorted")
        void breakdownByDate() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", simulationRunId)
                            .param("by", "DATE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5))) // 5 days
                    .andExpect(jsonPath("$[0].date").value("2025-01-01"))
                    .andExpect(jsonPath("$[4].date").value("2025-01-05"));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns 400 when 'by' parameter is missing")
        void returns400WhenByMissing() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", simulationRunId))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns 400 with message for invalid 'by' parameter")
        void returns400ForInvalidByParam() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", simulationRunId)
                            .param("by", "INVALID"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("Invalid 'by' parameter")))
                    .andExpect(jsonPath("$.detail").value(containsString("STORE")))
                    .andExpect(jsonPath("$.detail").value(containsString("SKU")))
                    .andExpect(jsonPath("$.detail").value(containsString("DATE")));
        }
    }

    @Nested
    @DisplayName("GET /api/simulation-runs/{runId}/export.csv")
    class CsvExportEndpoint {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("Returns CSV with correct headers and content type")
        void returnsCsvWithCorrectHeaders() throws Exception {
            mockMvc.perform(get("/api/simulation-runs/{runId}/export.csv", simulationRunId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(header().string("Content-Disposition",
                            containsString("attachment")))
                    .andExpect(header().string("Content-Disposition",
                            containsString(simulationRunId.toString())));
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("CSV contains correct number of rows (days × scope × 2 variants)")
        void csvContainsCorrectRowCount() throws Exception {
            // 5 days × 1 scope × 2 variants = 10 data rows + 1 header = 11 lines
            String csvContent = mockMvc.perform(get("/api/simulation-runs/{runId}/export.csv", simulationRunId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String[] lines = csvContent.split("\n");
            assertThat(lines).hasSize(11); // 1 header + 10 data rows
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("CSV header matches expected columns")
        void csvHeaderMatchesExpectedColumns() throws Exception {
            String csvContent = mockMvc.perform(get("/api/simulation-runs/{runId}/export.csv", simulationRunId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String header = csvContent.split("\n")[0];
            assertThat(header).isEqualTo(
                    "runId,experimentId,date,storeId,skuId,variant,basePrice,price,unitCost,units,revenue,margin");
        }

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("CSV contains CONTROL and TEST variants")
        void csvContainsBothVariants() throws Exception {
            String csvContent = mockMvc.perform(get("/api/simulation-runs/{runId}/export.csv", simulationRunId))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertThat(csvContent).contains("CONTROL");
            assertThat(csvContent).contains("TEST");
        }
    }

    @Nested
    @DisplayName("Negative Tests")
    class NegativeTests {

        @Test
        @WithMockUser(roles = "ANALYST")
        @DisplayName("All endpoints return 404 for unknown run ID")
        void allEndpointsReturn404ForUnknownRun() throws Exception {
            UUID unknownId = UUID.randomUUID();

            mockMvc.perform(get("/api/simulation-runs/{runId}/status", unknownId))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/simulation-runs/{runId}/summary", unknownId))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/simulation-runs/{runId}/timeseries", unknownId))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/simulation-runs/{runId}/breakdown", unknownId)
                            .param("by", "STORE"))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/simulation-runs/{runId}/export.csv", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Service Layer Direct Tests")
    class ServiceLayerTests {

        @Test
        @DisplayName("QueryService throws ResourceNotFoundException for unknown run")
        void queryServiceThrowsForUnknownRun() {
            UUID unknownId = UUID.randomUUID();

            assertThatThrownBy(() -> queryService.getRunStatus(unknownId))
                    .isInstanceOf(com.example.pricinglab.common.exception.ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("CSV export writes correct content")
        void csvExportWritesCorrectContent() throws Exception {
            StringWriter writer = new StringWriter();
            queryService.exportToCsv(simulationRunId, writer);
            String csv = writer.toString();

            // Verify header
            assertThat(csv).startsWith("runId,experimentId,date,storeId,skuId,variant,");

            // Verify data is present
            assertThat(csv).contains(simulationRunId.toString());
            assertThat(csv).contains(experiment.getId().toString());
            assertThat(csv).contains("2025-01-01");
            assertThat(csv).contains("CONTROL");
            assertThat(csv).contains("TEST");
        }
    }
}
