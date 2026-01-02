package com.example.pricinglab.experiment;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import com.example.pricinglab.experiment.repository.ExperimentLeverRepository;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.repository.ExperimentScopeRepository;
import com.example.pricinglab.experiment.service.ExperimentService;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Nasty test cases for experiment submit validation (Story 2.5).
 *
 * Tests 10 edge cases to verify:
 * 1. Exact error messages match specification
 * 2. HTTP status codes are correct
 * 3. Validation order is deterministic
 * 4. Multi-invalid scenarios return the FIRST validation failure
 *
 * Validation order in submitForApproval():
 * 1. Status check (must be DRAFT)
 * 2. Date validation (startDate/endDate non-null, endDate > startDate)
 * 3. Scope validation (at least one entry)
 * 4. Lever validation (must be configured)
 * 5. Guardrails validation (must be configured and valid)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExperimentSubmitNastyCasesTest {

    @Autowired
    private ExperimentService experimentService;

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
    private EntityManager entityManager;

    private Store store;
    private Sku sku;

    @BeforeEach
    void setUp() {
        // Create test store
        store = new Store(UUID.randomUUID(), "NASTY001", "Nasty Test Store");
        store.setRegion("Bangkok");
        store.setFormat("Hypermarket");
        store = storeRepository.save(store);

        // Create test SKU
        sku = new Sku(UUID.randomUUID(), "NASTY-SKU-001", "Nasty Test Product");
        sku.setCategory("Beverages");
        sku.setBrand("Test Brand");
        sku = skuRepository.save(sku);

        // Create base price for guardrails validation
        BasePrice basePrice = new BasePrice(
                UUID.randomUUID(), sku, store,
                new BigDecimal("100.00"), LocalDate.now().minusDays(30));
        basePriceRepository.save(basePrice);

        entityManager.flush();
    }

    // ============================================================
    // CASE 1: Non-existent experiment (404)
    // ============================================================
    @Nested
    @DisplayName("Case 1: Non-existent Experiment")
    class Case1_NonExistentExperiment {

        @Test
        @DisplayName("Submit non-existent experiment returns 404 with exact message")
        void submitNonExistentExperiment_Returns404() {
            UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000001");

            assertThatThrownBy(() -> experimentService.submitForApproval(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Experiment not found with id: " + nonExistentId);
        }
    }

    // ============================================================
    // CASE 2: Status = PENDING_APPROVAL (not DRAFT)
    // ============================================================
    @Nested
    @DisplayName("Case 2: Status PENDING_APPROVAL")
    class Case2_StatusPendingApproval {

        @Test
        @DisplayName("Submit PENDING_APPROVAL experiment fails with exact state error")
        void submitPendingApprovalExperiment_FailsWithStateError() {
            Experiment experiment = createExperiment(ExperimentStatus.PENDING_APPROVAL);

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(InvalidExperimentStateException.class)
                    .hasMessage("Cannot submit experiment in state PENDING_APPROVAL. " +
                            "Only experiments in state DRAFT can be submitted.");
        }
    }

    // ============================================================
    // CASE 3: Status = APPROVED
    // ============================================================
    @Nested
    @DisplayName("Case 3: Status APPROVED")
    class Case3_StatusApproved {

        @Test
        @DisplayName("Submit APPROVED experiment fails with exact state error")
        void submitApprovedExperiment_FailsWithStateError() {
            Experiment experiment = createExperiment(ExperimentStatus.APPROVED);

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(InvalidExperimentStateException.class)
                    .hasMessage("Cannot submit experiment in state APPROVED. " +
                            "Only experiments in state DRAFT can be submitted.");
        }
    }

    // ============================================================
    // CASE 4: startDate is null
    // ============================================================
    @Nested
    @DisplayName("Case 4: startDate Null")
    class Case4_StartDateNull {

        @Test
        @DisplayName("Submit with null startDate fails with exact message")
        void submitWithNullStartDate_FailsWithExactMessage() {
            Experiment experiment = createExperimentWithNullStartDate();

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment: startDate is required.");
        }
    }

    // ============================================================
    // CASE 5: endDate is null
    // ============================================================
    @Nested
    @DisplayName("Case 5: endDate Null")
    class Case5_EndDateNull {

        @Test
        @DisplayName("Submit with null endDate fails with exact message")
        void submitWithNullEndDate_FailsWithExactMessage() {
            Experiment experiment = createExperimentWithNullEndDate();

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment: endDate is required.");
        }
    }

    // ============================================================
    // CASE 6: endDate equals startDate (not after)
    // ============================================================
    @Nested
    @DisplayName("Case 6: endDate equals startDate")
    class Case6_EndDateEqualsStartDate {

        @Test
        @DisplayName("Submit with endDate==startDate fails with exact message including dates")
        void submitWithEqualDates_FailsWithExactMessage() {
            LocalDate sameDate = LocalDate.of(2025, 6, 15);
            Experiment experiment = createExperimentWithDates(sameDate, sameDate);

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment: endDate (2025-06-15) must be after startDate (2025-06-15).");
        }
    }

    // ============================================================
    // CASE 7: No scope entries (dates valid)
    // ============================================================
    @Nested
    @DisplayName("Case 7: No Scope Entries")
    class Case7_NoScopeEntries {

        @Test
        @DisplayName("Submit without scope entries fails with exact message")
        void submitWithoutScope_FailsWithExactMessage() {
            Experiment experiment = createValidDraftExperiment();
            // Intentionally NO scope added

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment without scope entries. Add at least one store+SKU pair.");
        }
    }

    // ============================================================
    // CASE 8: No lever (scope exists, dates valid)
    // ============================================================
    @Nested
    @DisplayName("Case 8: No Lever Configured")
    class Case8_NoLever {

        @Test
        @DisplayName("Submit without lever fails with exact message")
        void submitWithoutLever_FailsWithExactMessage() {
            Experiment experiment = createValidDraftExperiment();
            addScope(experiment);
            // Intentionally NO lever added

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment without a pricing lever configured.");
        }
    }

    // ============================================================
    // CASE 9: No guardrails (lever and scope exist)
    // ============================================================
    @Nested
    @DisplayName("Case 9: No Guardrails Configured")
    class Case9_NoGuardrails {

        @Test
        @DisplayName("Submit without guardrails fails with exact message")
        void submitWithoutGuardrails_FailsWithExactMessage() {
            Experiment experiment = createValidDraftExperiment();
            addScope(experiment);
            addLever(experiment);
            // Intentionally NO guardrails added

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment without guardrails configured.");
        }
    }

    // ============================================================
    // CASE 10: Multiple invalid conditions (ordering proof)
    // endDate < startDate AND no scope AND no lever AND no guardrails
    // Expected: Fails on date validation FIRST (validation order)
    // ============================================================
    @Nested
    @DisplayName("Case 10: Multiple Invalid Conditions - Ordering Proof")
    class Case10_MultipleInvalidConditions {

        @Test
        @DisplayName("Multiple invalid conditions: fails on DATE validation first (ordering)")
        void multipleInvalidConditions_FailsOnDateFirst() {
            // Create experiment with endDate BEFORE startDate
            LocalDate startDate = LocalDate.of(2025, 12, 31);
            LocalDate endDate = LocalDate.of(2025, 1, 1);
            Experiment experiment = createExperimentWithDates(startDate, endDate);
            // NO scope, NO lever, NO guardrails

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment: endDate (2025-01-01) must be after startDate (2025-12-31).");
        }

        @Test
        @DisplayName("Multiple invalid: scope+lever+guardrails missing, valid dates - fails on SCOPE first")
        void multipleInvalidButValidDates_FailsOnScopeFirst() {
            Experiment experiment = createValidDraftExperiment();
            // NO scope, NO lever, NO guardrails - but dates ARE valid

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment without scope entries. Add at least one store+SKU pair.");
        }

        @Test
        @DisplayName("Multiple invalid: lever+guardrails missing, scope exists - fails on LEVER first")
        void leverAndGuardrailsMissing_FailsOnLeverFirst() {
            Experiment experiment = createValidDraftExperiment();
            addScope(experiment);
            // NO lever, NO guardrails

            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot submit experiment without a pricing lever configured.");
        }
    }

    // --- Helper Methods ---

    private Experiment createExperiment(ExperimentStatus status) {
        Experiment experiment = new Experiment(
                UUID.randomUUID(),
                "Nasty Test Experiment",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(30)
        );
        experiment.setStatus(status);
        experiment.setDescription("Nasty test case");
        return experimentRepository.save(experiment);
    }

    private Experiment createValidDraftExperiment() {
        return createExperiment(ExperimentStatus.DRAFT);
    }

    private Experiment createExperimentWithNullStartDate() {
        Experiment experiment = new Experiment(
                UUID.randomUUID(),
                "Nasty Null StartDate Test",
                null, // startDate null
                LocalDate.now().plusDays(30)
        );
        experiment.setStatus(ExperimentStatus.DRAFT);
        return experimentRepository.save(experiment);
    }

    private Experiment createExperimentWithNullEndDate() {
        Experiment experiment = new Experiment(
                UUID.randomUUID(),
                "Nasty Null EndDate Test",
                LocalDate.now().plusDays(1),
                null // endDate null
        );
        experiment.setStatus(ExperimentStatus.DRAFT);
        return experimentRepository.save(experiment);
    }

    private Experiment createExperimentWithDates(LocalDate startDate, LocalDate endDate) {
        Experiment experiment = new Experiment(
                UUID.randomUUID(),
                "Nasty Date Test",
                startDate,
                endDate
        );
        experiment.setStatus(ExperimentStatus.DRAFT);
        return experimentRepository.save(experiment);
    }

    private void addScope(Experiment experiment) {
        ExperimentScope scope = new ExperimentScope(UUID.randomUUID(), store, sku, true);
        scope.setExperiment(experiment);
        scopeRepository.save(scope);
        entityManager.flush();
    }

    private void addLever(Experiment experiment) {
        ExperimentLever lever = new ExperimentLever(
                UUID.randomUUID(), sku, LeverType.PRICE_DISCOUNT, new BigDecimal("10.0"));
        lever.setExperiment(experiment);
        leverRepository.save(lever);
        entityManager.flush();
    }
}
