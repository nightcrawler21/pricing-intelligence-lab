package com.example.pricinglab.experiment;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import com.example.pricinglab.experiment.domain.ExperimentScope;
import com.example.pricinglab.experiment.dto.GuardrailsRequest;
import com.example.pricinglab.experiment.repository.ExperimentLeverRepository;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.repository.ExperimentScopeRepository;
import com.example.pricinglab.experiment.service.ExperimentGuardrailsService;
import com.example.pricinglab.experiment.service.ExperimentService;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
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
 * Integration tests for experiment submit validation (Story 2.5).
 *
 * Tests validate the submit gate enforces all requirements:
 * - Experiment must be in DRAFT status
 * - endDate > startDate
 * - At least one scope entry exists
 * - A lever is configured
 * - Guardrails are configured
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExperimentSubmitValidationTest {

    @Autowired
    private ExperimentService experimentService;

    @Autowired
    private ExperimentRepository experimentRepository;

    @Autowired
    private ExperimentScopeRepository scopeRepository;

    @Autowired
    private ExperimentLeverRepository leverRepository;

    @Autowired
    private ExperimentGuardrailsService guardrailsService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private BasePriceRepository basePriceRepository;

    @Autowired
    private EntityManager entityManager;

    private Experiment experiment;
    private Store store;
    private Sku sku;

    @BeforeEach
    void setUp() {
        // Create test store
        store = new Store(
                UUID.randomUUID(),
                "TEST001",
                "Test Store"
        );
        store.setRegion("Bangkok");
        store.setFormat("Hypermarket");
        store = storeRepository.save(store);

        // Create test SKU
        sku = new Sku(
                UUID.randomUUID(),
                "TEST-SKU-001",
                "Test Product"
        );
        sku.setCategory("Beverages");
        sku.setBrand("Test Brand");
        sku = skuRepository.save(sku);

        // Create base price (needed for guardrails validation)
        BasePrice basePrice = new BasePrice(
                UUID.randomUUID(),
                sku,
                store,
                new BigDecimal("45.00"),
                LocalDate.now().minusDays(30)
        );
        basePriceRepository.save(basePrice);

        // Create a base experiment in DRAFT status
        experiment = new Experiment(
                UUID.randomUUID(),
                "Test Experiment",
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(30)
        );
        experiment.setStatus(ExperimentStatus.DRAFT);
        experiment.setDescription("Test experiment for submit validation");
        experiment = experimentRepository.save(experiment);

        // Flush to ensure all entities are persisted before tests run
        entityManager.flush();
    }

    @Nested
    @DisplayName("Date Validation")
    class DateValidation {

        @Test
        @DisplayName("Submit fails when endDate equals startDate")
        void submitFailsWhenEndDateEqualsStartDate() {
            // Given: experiment with endDate == startDate
            // Date validation happens before scope/lever/guardrails checks
            LocalDate sameDate = LocalDate.now().plusDays(10);
            experiment.setStartDate(sameDate);
            experiment.setEndDate(sameDate);
            experimentRepository.save(experiment);

            // When/Then: submit should fail at date validation
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endDate")
                    .hasMessageContaining("must be after startDate");
        }

        @Test
        @DisplayName("Submit fails when endDate is before startDate")
        void submitFailsWhenEndDateBeforeStartDate() {
            // Given: experiment with endDate < startDate
            experiment.setStartDate(LocalDate.now().plusDays(20));
            experiment.setEndDate(LocalDate.now().plusDays(10));
            experimentRepository.save(experiment);

            // When/Then: submit should fail at date validation
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endDate")
                    .hasMessageContaining("must be after startDate");
        }

        @Test
        @DisplayName("Submit fails when startDate is null")
        void submitFailsWhenStartDateIsNull() {
            // Given: experiment with null startDate
            experiment.setStartDate(null);
            experimentRepository.save(experiment);

            // When/Then: submit should fail at date validation
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startDate is required");
        }

        @Test
        @DisplayName("Submit fails when endDate is null")
        void submitFailsWhenEndDateIsNull() {
            // Given: experiment with null endDate
            experiment.setEndDate(null);
            experimentRepository.save(experiment);

            // When/Then: submit should fail at date validation
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endDate is required");
        }
    }

    @Nested
    @DisplayName("Scope Validation")
    class ScopeValidation {

        @Test
        @DisplayName("Submit fails when no scope entries exist")
        void submitFailsWhenNoScopeEntries() {
            // Given: experiment with valid dates but NO scope
            // Scope validation happens after date validation
            // Intentionally NOT adding scope

            // When/Then: submit should fail with specific message
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot submit experiment without scope entries")
                    .hasMessageContaining("Add at least one store+SKU pair");
        }
    }

    @Nested
    @DisplayName("Lever Validation")
    class LeverValidation {

        @Test
        @DisplayName("Submit fails when no lever is configured")
        void submitFailsWhenNoLever() {
            // Given: experiment with valid dates and scope but NO lever
            addScope();
            // Intentionally NOT adding lever

            // When/Then: submit should fail with specific message
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot submit experiment without a pricing lever configured");
        }
    }

    @Nested
    @DisplayName("Guardrails Validation")
    class GuardrailsValidation {

        @Test
        @DisplayName("Submit fails when no guardrails are configured")
        void submitFailsWhenNoGuardrails() {
            // Given: experiment with valid dates, scope, lever but NO guardrails
            addScope();
            addLever();
            // Intentionally NOT adding guardrails

            // When/Then: submit should fail with specific message
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot submit experiment without guardrails configured");
        }
    }

    @Nested
    @DisplayName("Status Validation")
    class StatusValidation {

        @Test
        @DisplayName("Submit fails when experiment is not in DRAFT status")
        void submitFailsWhenNotDraft() {
            // Given: experiment in PENDING_APPROVAL status (status check happens first)
            experiment.setStatus(ExperimentStatus.PENDING_APPROVAL);
            experimentRepository.save(experiment);

            // When/Then: submit should fail
            assertThatThrownBy(() -> experimentService.submitForApproval(experiment.getId()))
                    .hasMessageContaining("Cannot submit experiment")
                    .hasMessageContaining("PENDING_APPROVAL");
        }
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("Submit succeeds when all requirements are met")
        void submitSucceedsWithAllRequirements() {
            // Given: experiment with all requirements met
            addScope();
            addLever();
            addGuardrails();

            // When: submit
            Experiment result = experimentService.submitForApproval(experiment.getId());

            // Then: status transitions to PENDING_APPROVAL
            assertThat(result.getStatus()).isEqualTo(ExperimentStatus.PENDING_APPROVAL);

            // Verify persisted
            Experiment persisted = experimentRepository.findById(experiment.getId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(ExperimentStatus.PENDING_APPROVAL);
        }
    }

    // --- Helper methods ---

    private void addScope() {
        ExperimentScope scope = new ExperimentScope(UUID.randomUUID(), store, sku, true);
        scope.setExperiment(experiment);
        scopeRepository.save(scope);
    }

    private void addLever() {
        ExperimentLever lever = new ExperimentLever(
                UUID.randomUUID(),
                sku,
                LeverType.PRICE_DISCOUNT,
                new BigDecimal("10.0") // 10% discount
        );
        lever.setExperiment(experiment);
        leverRepository.save(lever);
    }

    private void addGuardrails() {
        // Flush to ensure experiment is persisted
        entityManager.flush();

        // Insert guardrails via native SQL to avoid @MapsId complications in test context.
        // The @MapsId relationship causes issues when setExperiment() tries to derive the ID
        // from a potentially proxied experiment entity within nested transactions.
        // This test validates submit logic, not guardrails configuration (tested elsewhere).
        entityManager.createNativeQuery(
                "INSERT INTO experiment_guardrails (experiment_id, price_floor, price_ceiling, max_change_percent, " +
                "prevent_below_cost, enforce_price_points, created_at, updated_at) " +
                "VALUES (:experimentId, :priceFloor, :priceCeiling, :maxChangePercent, " +
                "true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("experimentId", experiment.getId())
                .setParameter("priceFloor", new BigDecimal("30.0"))
                .setParameter("priceCeiling", new BigDecimal("60.0"))
                .setParameter("maxChangePercent", new BigDecimal("20.0"))
                .executeUpdate();

        entityManager.flush();
    }
}
