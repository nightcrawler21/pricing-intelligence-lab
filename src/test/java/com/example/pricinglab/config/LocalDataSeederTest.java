package com.example.pricinglab.config;

import com.example.pricinglab.reference.sales.BaselineDailySalesRepository;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.example.pricinglab.reference.sku.SkuCostRepository;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LocalDataSeeder (Story 2.8).
 *
 * <p>These tests verify:</p>
 * <ul>
 *   <li>Profile guard: Seeder does NOT run in "test" profile</li>
 *   <li>Determinism: Formula calculations are stable</li>
 * </ul>
 */
class LocalDataSeederTest {

    /**
     * Tests that verify the seeder is NOT active in the test profile.
     * This is critical for ensuring tests don't get polluted with seed data.
     */
    @Nested
    @DisplayName("Profile Guard Tests")
    @SpringBootTest
    @ActiveProfiles("test")
    class ProfileGuardTests {

        @Autowired
        private ApplicationContext applicationContext;

        @Autowired
        private StoreRepository storeRepository;

        @Autowired
        private SkuRepository skuRepository;

        @Autowired
        private BasePriceRepository basePriceRepository;

        @Autowired
        private SkuCostRepository skuCostRepository;

        @Autowired
        private BaselineDailySalesRepository baselineSalesRepository;

        @Test
        @DisplayName("LocalDataSeeder bean should NOT exist in test profile")
        void seederBeanNotPresentInTestProfile() {
            // The bean should not be created when profile is "test"
            boolean seederExists = applicationContext.containsBean("localDataSeeder");
            assertThat(seederExists)
                .as("LocalDataSeeder should not be present in test profile")
                .isFalse();
        }

        @Test
        @DisplayName("No seed stores should exist in test profile")
        void noSeedStoresInTestProfile() {
            // Verify no stores with seed prefix exist
            long seedStoreCount = storeRepository.findAll().stream()
                .filter(s -> s.getStoreCode().startsWith("SEED-"))
                .count();

            assertThat(seedStoreCount)
                .as("No seed stores should exist in test profile")
                .isZero();
        }

        @Test
        @DisplayName("No seed SKUs should exist in test profile")
        void noSeedSkusInTestProfile() {
            // Verify no SKUs with seed prefix exist
            long seedSkuCount = skuRepository.findAll().stream()
                .filter(s -> s.getSkuCode().startsWith("SEED-"))
                .count();

            assertThat(seedSkuCount)
                .as("No seed SKUs should exist in test profile")
                .isZero();
        }

        @Test
        @DisplayName("No baseline daily sales should exist from seeder in test profile")
        void noBaselineSalesInTestProfile() {
            // The baseline_daily_sales table should be empty (no seeded data)
            long totalCount = baselineSalesRepository.count();

            assertThat(totalCount)
                .as("No baseline sales should exist in test profile")
                .isZero();
        }
    }

    /**
     * Unit tests for the deterministic formula calculations.
     * These verify that the seeder produces consistent, predictable outputs.
     */
    @Nested
    @DisplayName("Determinism Tests")
    class DeterminismTests {

        @Test
        @DisplayName("Store count is deterministic")
        void storeCountIsDeterministic() {
            assertThat(LocalDataSeeder.getStoreCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("SKU count is deterministic")
        void skuCountIsDeterministic() {
            assertThat(LocalDataSeeder.getSkuCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("Baseline days count is deterministic")
        void baselineDaysIsDeterministic() {
            assertThat(LocalDataSeeder.getBaselineDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("Price formula is deterministic: basePrice = 20 + (skuIdx * 3) + (storeIdx * 2)")
        void priceFormulaIsDeterministic() {
            // Test various combinations
            assertThat(LocalDataSeeder.calculateExpectedPrice(0, 0))
                .isEqualByComparingTo(new BigDecimal("20.00")); // 20 + 0 + 0

            assertThat(LocalDataSeeder.calculateExpectedPrice(0, 1))
                .isEqualByComparingTo(new BigDecimal("23.00")); // 20 + 3 + 0

            assertThat(LocalDataSeeder.calculateExpectedPrice(1, 0))
                .isEqualByComparingTo(new BigDecimal("22.00")); // 20 + 0 + 2

            assertThat(LocalDataSeeder.calculateExpectedPrice(2, 5))
                .isEqualByComparingTo(new BigDecimal("39.00")); // 20 + 15 + 4

            assertThat(LocalDataSeeder.calculateExpectedPrice(4, 9))
                .isEqualByComparingTo(new BigDecimal("55.00")); // 20 + 27 + 8

            // Verify determinism by calling twice
            BigDecimal first = LocalDataSeeder.calculateExpectedPrice(3, 7);
            BigDecimal second = LocalDataSeeder.calculateExpectedPrice(3, 7);
            assertThat(first).isEqualByComparingTo(second);
        }

        @Test
        @DisplayName("Cost formula is deterministic: cost = (20 + skuIdx*3 + 4) * 0.6")
        void costFormulaIsDeterministic() {
            // SKU 0: (20 + 0 + 4) * 0.6 = 14.40
            assertThat(LocalDataSeeder.calculateExpectedCost(0))
                .isEqualByComparingTo(new BigDecimal("14.40"));

            // SKU 5: (20 + 15 + 4) * 0.6 = 23.40
            assertThat(LocalDataSeeder.calculateExpectedCost(5))
                .isEqualByComparingTo(new BigDecimal("23.40"));

            // SKU 9: (20 + 27 + 4) * 0.6 = 30.60
            assertThat(LocalDataSeeder.calculateExpectedCost(9))
                .isEqualByComparingTo(new BigDecimal("30.60"));

            // Verify determinism
            BigDecimal first = LocalDataSeeder.calculateExpectedCost(3);
            BigDecimal second = LocalDataSeeder.calculateExpectedCost(3);
            assertThat(first).isEqualByComparingTo(second);
        }

        @Test
        @DisplayName("Units formula is deterministic: units = 80 + (skuIdx * 5) + (dayOffset % 7) * 2")
        void unitsFormulaIsDeterministic() {
            // SKU 0, Day 0: 80 + 0 + 0 = 80
            assertThat(LocalDataSeeder.calculateExpectedUnits(0, 0)).isEqualTo(80);

            // SKU 0, Day 6: 80 + 0 + 12 = 92
            assertThat(LocalDataSeeder.calculateExpectedUnits(0, 6)).isEqualTo(92);

            // SKU 0, Day 7: 80 + 0 + 0 = 80 (week cycles)
            assertThat(LocalDataSeeder.calculateExpectedUnits(0, 7)).isEqualTo(80);

            // SKU 5, Day 3: 80 + 25 + 6 = 111
            assertThat(LocalDataSeeder.calculateExpectedUnits(5, 3)).isEqualTo(111);

            // SKU 9, Day 29: 80 + 45 + (29%7=1)*2 = 80 + 45 + 2 = 127
            assertThat(LocalDataSeeder.calculateExpectedUnits(9, 29)).isEqualTo(127);

            // Verify determinism
            int first = LocalDataSeeder.calculateExpectedUnits(4, 15);
            int second = LocalDataSeeder.calculateExpectedUnits(4, 15);
            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("Total seeded records are deterministic")
        void totalRecordsAreDeterministic() {
            int stores = LocalDataSeeder.getStoreCount();       // 5
            int skus = LocalDataSeeder.getSkuCount();           // 10
            int days = LocalDataSeeder.getBaselineDays();       // 30

            int expectedPrices = stores * skus;                  // 50
            int expectedCosts = skus;                            // 10
            int expectedSales = stores * skus * days;            // 1500

            assertThat(expectedPrices).isEqualTo(50);
            assertThat(expectedCosts).isEqualTo(10);
            assertThat(expectedSales).isEqualTo(1500);
        }

        @Test
        @DisplayName("Weekly pattern in baseline units creates predictable variation")
        void weeklyPatternCreatesVariation() {
            // Verify that the day offset creates a weekly cycle
            int skuIdx = 0;

            // Days 0-6 should show increasing pattern
            int day0 = LocalDataSeeder.calculateExpectedUnits(skuIdx, 0);
            int day1 = LocalDataSeeder.calculateExpectedUnits(skuIdx, 1);
            int day6 = LocalDataSeeder.calculateExpectedUnits(skuIdx, 6);

            assertThat(day1).isGreaterThan(day0);
            assertThat(day6).isGreaterThan(day1);

            // Day 7 should reset (same as day 0)
            int day7 = LocalDataSeeder.calculateExpectedUnits(skuIdx, 7);
            assertThat(day7).isEqualTo(day0);

            // Day 14 should also equal day 0
            int day14 = LocalDataSeeder.calculateExpectedUnits(skuIdx, 14);
            assertThat(day14).isEqualTo(day0);
        }
    }

    /**
     * Integration tests that prove running the seeder twice produces no duplicates.
     *
     * <p>Strategy:</p>
     * <ul>
     *   <li>Boot Spring context in "test" profile (seeder bean NOT auto-run)</li>
     *   <li>Manually instantiate LocalDataSeeder with autowired repositories</li>
     *   <li>Call seeder.run(null) twice</li>
     *   <li>Assert counts remain stable and match expected values</li>
     * </ul>
     */
    @Nested
    @DisplayName("Idempotency Tests")
    @SpringBootTest
    @ActiveProfiles("test")
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class IdempotencyTests {

        private static final String SEED_PREFIX = "SEED-";

        @Autowired
        private StoreRepository storeRepository;

        @Autowired
        private SkuRepository skuRepository;

        @Autowired
        private BasePriceRepository basePriceRepository;

        @Autowired
        private SkuCostRepository skuCostRepository;

        @Autowired
        private BaselineDailySalesRepository baselineSalesRepository;

        @Test
        @DisplayName("Running seeder twice produces identical row counts - no duplicates")
        void runningSeederTwiceProducesNoDuplicates() {
            // Manually instantiate seeder (bean not present in test profile)
            LocalDataSeeder seeder = new LocalDataSeeder(
                storeRepository,
                skuRepository,
                basePriceRepository,
                skuCostRepository,
                baselineSalesRepository
            );

            // === FIRST RUN ===
            seeder.run(null);

            long storesAfterRun1 = storeRepository.countByStoreCodeStartingWith(SEED_PREFIX);
            long skusAfterRun1 = skuRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long pricesAfterRun1 = basePriceRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long costsAfterRun1 = skuCostRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long salesAfterRun1 = baselineSalesRepository.countBySkuCodeStartingWith(SEED_PREFIX);

            // Verify first run produced expected counts
            assertThat(storesAfterRun1)
                .as("Stores after run 1")
                .isEqualTo(5);
            assertThat(skusAfterRun1)
                .as("SKUs after run 1")
                .isEqualTo(10);
            assertThat(pricesAfterRun1)
                .as("Base prices after run 1 (5 stores × 10 SKUs)")
                .isEqualTo(50);
            assertThat(costsAfterRun1)
                .as("SKU costs after run 1 (1 per SKU)")
                .isEqualTo(10);
            assertThat(salesAfterRun1)
                .as("Baseline daily sales after run 1 (5×10×30)")
                .isEqualTo(1500);

            // === SECOND RUN ===
            seeder.run(null);

            long storesAfterRun2 = storeRepository.countByStoreCodeStartingWith(SEED_PREFIX);
            long skusAfterRun2 = skuRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long pricesAfterRun2 = basePriceRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long costsAfterRun2 = skuCostRepository.countBySkuCodeStartingWith(SEED_PREFIX);
            long salesAfterRun2 = baselineSalesRepository.countBySkuCodeStartingWith(SEED_PREFIX);

            // Verify second run did NOT create duplicates
            assertThat(storesAfterRun2)
                .as("Stores after run 2 should equal run 1")
                .isEqualTo(storesAfterRun1)
                .isEqualTo(5);
            assertThat(skusAfterRun2)
                .as("SKUs after run 2 should equal run 1")
                .isEqualTo(skusAfterRun1)
                .isEqualTo(10);
            assertThat(pricesAfterRun2)
                .as("Base prices after run 2 should equal run 1")
                .isEqualTo(pricesAfterRun1)
                .isEqualTo(50);
            assertThat(costsAfterRun2)
                .as("SKU costs after run 2 should equal run 1")
                .isEqualTo(costsAfterRun1)
                .isEqualTo(10);
            assertThat(salesAfterRun2)
                .as("Baseline daily sales after run 2 should equal run 1")
                .isEqualTo(salesAfterRun1)
                .isEqualTo(1500);
        }

        @Test
        @DisplayName("Seeder produces stable UUIDs - same entity ID after multiple runs")
        void seederProducesStableUuids() {
            LocalDataSeeder seeder = new LocalDataSeeder(
                storeRepository,
                skuRepository,
                basePriceRepository,
                skuCostRepository,
                baselineSalesRepository
            );

            // First run
            seeder.run(null);

            // Capture the UUID of first seeded store
            Optional<Store> storeAfterRun1 = storeRepository.findByStoreCode("SEED-STR-001");
            assertThat(storeAfterRun1).isPresent();
            UUID storeIdRun1 = storeAfterRun1.get().getId();

            // Second run
            seeder.run(null);

            // Verify UUID is unchanged
            Optional<Store> storeAfterRun2 = storeRepository.findByStoreCode("SEED-STR-001");
            assertThat(storeAfterRun2).isPresent();
            UUID storeIdRun2 = storeAfterRun2.get().getId();

            assertThat(storeIdRun2)
                .as("Store UUID should remain stable across runs")
                .isEqualTo(storeIdRun1);
        }

        @Test
        @DisplayName("Seeder produces deterministic price values")
        void seederProducesDeterministicPriceValues() {
            LocalDataSeeder seeder = new LocalDataSeeder(
                storeRepository,
                skuRepository,
                basePriceRepository,
                skuCostRepository,
                baselineSalesRepository
            );

            // Run seeder
            seeder.run(null);

            // Find store 0 and SKU 0
            Store store0 = storeRepository.findByStoreCode("SEED-STR-001").orElseThrow();
            var sku0 = skuRepository.findBySkuCode("SEED-SKU-001").orElseThrow();

            // Find the base price for store 0, SKU 0
            // Expected: 20 + (skuIdx=0 * 3) + (storeIdx=0 * 2) = 20.00
            java.time.LocalDate effectiveDate = java.time.LocalDate.of(2024, 1, 1);
            Optional<BasePrice> basePrice = basePriceRepository.findEffectivePrice(
                sku0.getId(), store0.getId(), effectiveDate);

            assertThat(basePrice).isPresent();
            assertThat(basePrice.get().getPrice())
                .as("Base price for store 0, SKU 0 should be 20.00")
                .isEqualByComparingTo(new BigDecimal("20.00"));

            // Run seeder again
            seeder.run(null);

            // Verify price is unchanged
            Optional<BasePrice> basePriceAfterRun2 = basePriceRepository.findEffectivePrice(
                sku0.getId(), store0.getId(), effectiveDate);

            assertThat(basePriceAfterRun2).isPresent();
            assertThat(basePriceAfterRun2.get().getPrice())
                .as("Base price should remain 20.00 after second run")
                .isEqualByComparingTo(new BigDecimal("20.00"));

            // Verify it's the same record (same ID)
            assertThat(basePriceAfterRun2.get().getId())
                .as("Base price ID should be stable")
                .isEqualTo(basePrice.get().getId());
        }
    }
}
