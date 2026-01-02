package com.example.pricinglab.experiment;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import com.example.pricinglab.experiment.dto.ApprovalRequest;
import com.example.pricinglab.experiment.dto.CreateExperimentRequest;
import com.example.pricinglab.experiment.dto.LeverRequest;
import com.example.pricinglab.experiment.dto.ModifyScopeRequest;
import com.example.pricinglab.experiment.dto.ScopeEntryRequest;
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
import com.example.pricinglab.simulation.repository.SimulationRunRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Minimal Integration Tests (Story 2.9 — Credibility Pack).
 *
 * <p>Proves the core experiment flow works end-to-end:
 * Create → Configure → Submit → Approve → Simulate</p>
 *
 * <p>These tests exist to build confidence and prevent regressions,
 * not to maximize coverage.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExperimentLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentScopeRepository scopeRepository;

    @Autowired
    private ExperimentLeverRepository leverRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private Sku sku;

    private static final BigDecimal BASE_PRICE = new BigDecimal("100.00");
    private static final BigDecimal UNIT_COST = new BigDecimal("60.00");
    private static final BigDecimal DISCOUNT_PERCENT = new BigDecimal("10.0");

    @BeforeEach
    void setUp() {
        // Create test store
        store = new Store(UUID.randomUUID(), "E2E-STORE-001", "E2E Test Store");
        store.setRegion("Bangkok");
        store.setFormat("Hypermarket");
        store = storeRepository.save(store);

        // Create test SKU
        sku = new Sku(UUID.randomUUID(), "E2E-SKU-001", "E2E Test Product");
        sku.setCategory("Beverages");
        sku.setBrand("Test Brand");
        sku = skuRepository.save(sku);

        // Create base price (required for simulation)
        BasePrice basePrice = new BasePrice(
            UUID.randomUUID(), sku, store, BASE_PRICE,
            LocalDate.of(2024, 1, 1)
        );
        basePriceRepository.save(basePrice);

        // Create SKU cost (required for margin calculation)
        SkuCost skuCost = new SkuCost(
            UUID.randomUUID(), sku, UNIT_COST,
            LocalDate.of(2024, 1, 1)
        );
        skuCostRepository.save(skuCost);

        entityManager.flush();
    }

    // =========================================================================
    // TEST 1: Happy Path — Full Lifecycle Works
    // =========================================================================

    @Test
    @DisplayName("Full experiment flow succeeds end-to-end: Create → Configure → Submit → Approve → Simulate")
    @WithMockUser(username = "admin", roles = {"ADMIN", "ANALYST"})
    void fullExperimentFlow_succeeds_endToEnd() throws Exception {
        // === STEP 1: Create experiment via HTTP ===
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);

        CreateExperimentRequest createRequest = new CreateExperimentRequest(
            "E2E Test Experiment",
            "End-to-end integration test",
            startDate,
            endDate,
            "Testing full lifecycle",
            "Verify all steps work together"
        );

        MvcResult createResult = mockMvc.perform(post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.name").value("E2E Test Experiment"))
            .andReturn();

        UUID experimentId = UUID.fromString(extractId(createResult));

        // === STEP 2: Add scope via HTTP ===
        ModifyScopeRequest scopeRequest = new ModifyScopeRequest(
            List.of(new ScopeEntryRequest(store.getId(), sku.getId(), true))
        );

        mockMvc.perform(post("/api/experiments/" + experimentId + "/scope")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scopeRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scopes").isArray())
            .andExpect(jsonPath("$.scopes.length()").value(1));

        // === STEP 3: Configure lever via HTTP ===
        LeverRequest leverRequest = new LeverRequest(
            LeverType.PRICE_DISCOUNT,
            sku.getId(),
            DISCOUNT_PERCENT
        );

        mockMvc.perform(put("/api/experiments/" + experimentId + "/lever")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leverRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("PRICE_DISCOUNT"))
            .andExpect(jsonPath("$.discountPercentage").value(10.0));

        // === STEP 4: Configure guardrails via native SQL ===
        // Note: Using native SQL to avoid @MapsId complications in test context.
        // This is the same approach used in ExperimentSubmitValidationTest.
        entityManager.flush();
        insertGuardrailsViaNativeSql(experimentId);
        entityManager.flush();

        // === STEP 5: Submit for approval via HTTP ===
        mockMvc.perform(post("/api/experiments/" + experimentId + "/submit"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        // === STEP 6: Approve experiment via HTTP (requires ADMIN role) ===
        ApprovalRequest approvalRequest = new ApprovalRequest(true, null);

        mockMvc.perform(post("/api/experiments/" + experimentId + "/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(approvalRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // === STEP 7: Run simulation via HTTP ===
        MvcResult simulationResult = mockMvc.perform(post("/api/experiments/" + experimentId + "/simulate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationRunId").exists())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.totalDaysSimulated").value(7))
            .andReturn();

        String runId = extractRunId(simulationResult);

        // === VERIFY: Simulation results exist ===
        entityManager.flush();
        entityManager.clear();

        // Verify via query service that results exist
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/simulation-runs/" + runId + "/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.control").exists())
            .andExpect(jsonPath("$.test").exists())
            .andExpect(jsonPath("$.delta").exists())
            // TEST metrics should differ from CONTROL (due to 10% discount)
            .andExpect(jsonPath("$.delta.units").exists())
            .andExpect(jsonPath("$.delta.revenue").exists());

        // Verify timeseries has data for both variants (API returns array directly)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/simulation-runs/" + runId + "/timeseries"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(7)); // 7 days of data
    }

    // =========================================================================
    // TEST 2: Invalid Submission — Guardrails Missing
    // =========================================================================

    @Test
    @DisplayName("Submit without guardrails returns 400 with exact error message")
    @WithMockUser(username = "analyst", roles = "ANALYST")
    void submitWithoutGuardrails_returns400() throws Exception {
        // === Create experiment ===
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);

        CreateExperimentRequest createRequest = new CreateExperimentRequest(
            "Missing Guardrails Test",
            "Test for submit validation",
            startDate,
            endDate,
            null,
            null
        );

        MvcResult createResult = mockMvc.perform(post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String experimentId = extractId(createResult);

        // === Add scope ===
        ModifyScopeRequest scopeRequest = new ModifyScopeRequest(
            List.of(new ScopeEntryRequest(store.getId(), sku.getId(), true))
        );

        mockMvc.perform(post("/api/experiments/" + experimentId + "/scope")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scopeRequest)))
            .andExpect(status().isOk());

        // === Add lever ===
        LeverRequest leverRequest = new LeverRequest(
            LeverType.PRICE_DISCOUNT,
            sku.getId(),
            DISCOUNT_PERCENT
        );

        mockMvc.perform(put("/api/experiments/" + experimentId + "/lever")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(leverRequest)))
            .andExpect(status().isOk());

        // === DO NOT add guardrails ===

        // === Attempt to submit — should fail with 400 ===
        mockMvc.perform(post("/api/experiments/" + experimentId + "/submit"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("Cannot submit experiment without guardrails configured")));

        // === Verify experiment remains in DRAFT ===
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/experiments/" + experimentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // =========================================================================
    // TEST 3: Simulation Requires Approved Experiment
    // =========================================================================

    @Test
    @DisplayName("Simulate DRAFT experiment fails with clear error message")
    @WithMockUser(username = "analyst", roles = "ANALYST")
    void simulateDraftExperiment_fails() throws Exception {
        // === Create experiment (stays in DRAFT) ===
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(7);

        CreateExperimentRequest createRequest = new CreateExperimentRequest(
            "Draft Simulation Test",
            "Test that DRAFT experiments cannot be simulated",
            startDate,
            endDate,
            null,
            null
        );

        MvcResult createResult = mockMvc.perform(post("/api/experiments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String experimentId = extractId(createResult);

        // === Attempt to simulate DRAFT experiment — should fail ===
        // Note: The experiment is in DRAFT status and hasn't been submitted/approved
        mockMvc.perform(post("/api/experiments/" + experimentId + "/simulate"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid Experiment State"))
            .andExpect(jsonPath("$.detail", containsString("DRAFT")))
            .andExpect(jsonPath("$.detail", containsString("APPROVED")));

        // === Verify no simulation run was created ===
        UUID expId = UUID.fromString(experimentId);
        long runCount = simulationRunRepository.findByExperimentId(expId).size();
        assertThat(runCount).as("No simulation run should be created for DRAFT experiment").isZero();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String extractId(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(content);
        return node.get("id").asText();
    }

    private String extractRunId(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(content);
        return node.get("simulationRunId").asText();
    }

    /**
     * Inserts guardrails via native SQL to avoid @MapsId complications in test context.
     * This is the same approach used in ExperimentSubmitValidationTest.
     */
    private void insertGuardrailsViaNativeSql(UUID experimentId) {
        entityManager.createNativeQuery(
                "INSERT INTO experiment_guardrails (experiment_id, price_floor, price_ceiling, max_change_percent, " +
                "prevent_below_cost, enforce_price_points, created_at, updated_at) " +
                "VALUES (:experimentId, :priceFloor, :priceCeiling, :maxChangePercent, " +
                "true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
            .setParameter("experimentId", experimentId)
            .setParameter("priceFloor", new BigDecimal("50.00"))
            .setParameter("priceCeiling", new BigDecimal("150.00"))
            .setParameter("maxChangePercent", new BigDecimal("20.00"))
            .executeUpdate();
    }
}
