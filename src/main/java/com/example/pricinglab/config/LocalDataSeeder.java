package com.example.pricinglab.config;

import com.example.pricinglab.reference.sales.BaselineDailySales;
import com.example.pricinglab.reference.sales.BaselineDailySalesRepository;
import com.example.pricinglab.reference.sku.BasePrice;
import com.example.pricinglab.reference.sku.BasePriceRepository;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.sku.SkuCost;
import com.example.pricinglab.reference.sku.SkuCostRepository;
import com.example.pricinglab.reference.sku.SkuRepository;
import com.example.pricinglab.reference.store.Store;
import com.example.pricinglab.reference.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Local-only data seeder that populates reference data on application startup.
 *
 * <p><b>IMPORTANT:</b> This seeder runs ONLY in the "local" profile.
 * It will NOT run in test, dev, qa, or prod profiles.</p>
 *
 * <h2>Seeded Data:</h2>
 * <ul>
 *   <li>5 stores with stable UUIDs</li>
 *   <li>10 SKUs with stable UUIDs</li>
 *   <li>50 base prices (5 stores × 10 SKUs)</li>
 *   <li>10 SKU costs (1 per SKU)</li>
 *   <li>1500 baseline daily sales (5 stores × 10 SKUs × 30 days)</li>
 * </ul>
 *
 * <h2>Determinism:</h2>
 * All data is generated using deterministic formulas based on:
 * <ul>
 *   <li>Stable UUID seeds (hash-based from constant strings)</li>
 *   <li>Store index and SKU index for price/cost calculations</li>
 *   <li>Day offset for baseline sales variation</li>
 * </ul>
 *
 * <h2>Idempotency:</h2>
 * Uses stable UUIDs with saveAll() to upsert data. Running multiple times
 * produces the same result without duplicates.
 *
 * @see Profile
 */
@Component
@Profile("local")
@Order(100) // Run after Flyway migrations
public class LocalDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);

    // ========================================================================
    // SEED CONFIGURATION
    // ========================================================================

    /**
     * Fixed date range for baseline sales data.
     * Uses a 30-day period starting from 2024-01-01.
     */
    private static final LocalDate BASELINE_START_DATE = LocalDate.of(2024, 1, 1);
    private static final int BASELINE_DAYS = 30;

    /**
     * Effective date for all prices and costs.
     */
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2024, 1, 1);

    // ========================================================================
    // STABLE UUID NAMESPACES (deterministic seed)
    // ========================================================================

    /**
     * UUID namespace for generating stable store IDs.
     * Generated from UUID.nameUUIDFromBytes("pricing-lab-store-seed".getBytes())
     */
    private static final UUID STORE_NAMESPACE = UUID.fromString("a1b2c3d4-e5f6-4a5b-8c7d-9e0f1a2b3c4d");

    /**
     * UUID namespace for generating stable SKU IDs.
     */
    private static final UUID SKU_NAMESPACE = UUID.fromString("b2c3d4e5-f6a7-5b6c-9d8e-0f1a2b3c4d5e");

    /**
     * UUID namespace for generating stable base price IDs.
     */
    private static final UUID PRICE_NAMESPACE = UUID.fromString("c3d4e5f6-a7b8-6c7d-0e9f-1a2b3c4d5e6f");

    /**
     * UUID namespace for generating stable cost IDs.
     */
    private static final UUID COST_NAMESPACE = UUID.fromString("d4e5f6a7-b8c9-7d8e-1f0a-2b3c4d5e6f70");

    /**
     * UUID namespace for generating stable baseline sales IDs.
     */
    private static final UUID SALES_NAMESPACE = UUID.fromString("e5f6a7b8-c9d0-8e9f-2a1b-3c4d5e6f7081");

    // ========================================================================
    // STORE DEFINITIONS
    // ========================================================================

    private static final String[][] STORE_DATA = {
        // {code, name, region, format, address}
        {"SEED-STR-001", "Central Bangkok Hypermarket", "Bangkok", "Hypermarket", "123 Sukhumvit Road, Bangkok 10110"},
        {"SEED-STR-002", "Rama 9 Supermarket", "Bangkok", "Supermarket", "456 Rama 9 Road, Bangkok 10310"},
        {"SEED-STR-003", "Chiang Mai Central", "North", "Hypermarket", "789 Huay Kaew Road, Chiang Mai 50200"},
        {"SEED-STR-004", "Phuket Beach Store", "South", "Supermarket", "321 Patong Beach Road, Phuket 83150"},
        {"SEED-STR-005", "Khon Kaen Express", "Northeast", "Express", "654 Mittraphap Road, Khon Kaen 40000"}
    };

    // ========================================================================
    // SKU DEFINITIONS
    // ========================================================================

    private static final String[][] SKU_DATA = {
        // {code, name, category, subcategory, brand, uom}
        {"SEED-SKU-001", "Seed Cola 1.5L", "Beverages", "Carbonated Drinks", "SeedCola", "EA"},
        {"SEED-SKU-002", "Seed Water 6L", "Beverages", "Water", "SeedWater", "EA"},
        {"SEED-SKU-003", "Seed Energy Drink 250ml", "Beverages", "Energy Drinks", "SeedEnergy", "EA"},
        {"SEED-SKU-004", "Seed Chips Classic 75g", "Snacks", "Chips", "SeedChips", "EA"},
        {"SEED-SKU-005", "Seed Cookies 150g", "Snacks", "Biscuits", "SeedCookies", "EA"},
        {"SEED-SKU-006", "Seed Fresh Milk 1L", "Dairy", "Fresh Milk", "SeedDairy", "EA"},
        {"SEED-SKU-007", "Seed Yogurt 135ml", "Dairy", "Yogurt", "SeedDairy", "EA"},
        {"SEED-SKU-008", "Seed Instant Noodles 55g", "Instant Food", "Instant Noodles", "SeedNoodles", "EA"},
        {"SEED-SKU-009", "Seed Rice 5kg", "Staples", "Rice", "SeedRice", "EA"},
        {"SEED-SKU-010", "Seed Cooking Oil 1L", "Staples", "Cooking Oil", "SeedOil", "EA"}
    };

    // ========================================================================
    // REPOSITORIES
    // ========================================================================

    private final StoreRepository storeRepository;
    private final SkuRepository skuRepository;
    private final BasePriceRepository basePriceRepository;
    private final SkuCostRepository skuCostRepository;
    private final BaselineDailySalesRepository baselineSalesRepository;

    public LocalDataSeeder(
            StoreRepository storeRepository,
            SkuRepository skuRepository,
            BasePriceRepository basePriceRepository,
            SkuCostRepository skuCostRepository,
            BaselineDailySalesRepository baselineSalesRepository) {
        this.storeRepository = storeRepository;
        this.skuRepository = skuRepository;
        this.basePriceRepository = basePriceRepository;
        this.skuCostRepository = skuCostRepository;
        this.baselineSalesRepository = baselineSalesRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("LOCAL DATA SEEDER - Starting");
        log.info("========================================");

        // Seed in order of dependencies
        List<Store> stores = seedStores();
        List<Sku> skus = seedSkus();
        seedBasePrices(stores, skus);
        seedSkuCosts(skus);
        seedBaselineDailySales(stores, skus);

        log.info("========================================");
        log.info("LOCAL DATA SEEDER - Complete");
        log.info("Seeded: {} stores, {} SKUs, {} base prices, {} costs, {} baseline sales",
            stores.size(),
            skus.size(),
            stores.size() * skus.size(),
            skus.size(),
            stores.size() * skus.size() * BASELINE_DAYS);
        log.info("========================================");
    }

    // ========================================================================
    // SEED METHODS
    // ========================================================================

    private List<Store> seedStores() {
        log.info("Seeding {} stores...", STORE_DATA.length);
        List<Store> stores = new ArrayList<>();

        for (int i = 0; i < STORE_DATA.length; i++) {
            String[] data = STORE_DATA[i];
            UUID stableId = generateStableUUID(STORE_NAMESPACE, "store-" + i);

            Store store = new Store(stableId, data[0], data[1]);
            store.setRegion(data[2]);
            store.setFormat(data[3]);
            store.setAddress(data[4]);
            store.setActive(true);

            stores.add(store);
        }

        // saveAll with stable IDs = upsert behavior
        return storeRepository.saveAll(stores);
    }

    private List<Sku> seedSkus() {
        log.info("Seeding {} SKUs...", SKU_DATA.length);
        List<Sku> skus = new ArrayList<>();

        for (int i = 0; i < SKU_DATA.length; i++) {
            String[] data = SKU_DATA[i];
            UUID stableId = generateStableUUID(SKU_NAMESPACE, "sku-" + i);

            Sku sku = new Sku(stableId, data[0], data[1]);
            sku.setCategory(data[2]);
            sku.setSubcategory(data[3]);
            sku.setBrand(data[4]);
            sku.setUom(data[5]);
            sku.setActive(true);

            skus.add(sku);
        }

        return skuRepository.saveAll(skus);
    }

    private void seedBasePrices(List<Store> stores, List<Sku> skus) {
        log.info("Seeding {} base prices...", stores.size() * skus.size());
        List<BasePrice> prices = new ArrayList<>();

        for (int storeIdx = 0; storeIdx < stores.size(); storeIdx++) {
            Store store = stores.get(storeIdx);

            for (int skuIdx = 0; skuIdx < skus.size(); skuIdx++) {
                Sku sku = skus.get(skuIdx);
                UUID stableId = generateStableUUID(PRICE_NAMESPACE,
                    "price-" + storeIdx + "-" + skuIdx);

                // Deterministic price formula:
                // basePrice = 20 + (skuIndex * 3) + (storeIndex * 2)
                BigDecimal price = BigDecimal.valueOf(20 + (skuIdx * 3) + (storeIdx * 2))
                    .setScale(2, RoundingMode.HALF_UP);

                BasePrice basePrice = new BasePrice(stableId, sku, store, price, EFFECTIVE_DATE);
                prices.add(basePrice);
            }
        }

        basePriceRepository.saveAll(prices);
    }

    private void seedSkuCosts(List<Sku> skus) {
        log.info("Seeding {} SKU costs...", skus.size());
        List<SkuCost> costs = new ArrayList<>();

        for (int skuIdx = 0; skuIdx < skus.size(); skuIdx++) {
            Sku sku = skus.get(skuIdx);
            UUID stableId = generateStableUUID(COST_NAMESPACE, "cost-" + skuIdx);

            // Deterministic cost formula:
            // Use the average base price for this SKU (across all stores)
            // avgPrice = 20 + (skuIndex * 3) + (avgStoreIndex * 2)
            // For 5 stores: avgStoreIndex = 2
            // Then: cost = avgPrice * 0.6
            BigDecimal avgPrice = BigDecimal.valueOf(20 + (skuIdx * 3) + 4); // 4 = avg(0,1,2,3,4)*2
            BigDecimal cost = avgPrice.multiply(BigDecimal.valueOf(0.6))
                .setScale(2, RoundingMode.HALF_UP);

            SkuCost skuCost = new SkuCost(stableId, sku, cost, EFFECTIVE_DATE);
            costs.add(skuCost);
        }

        skuCostRepository.saveAll(costs);
    }

    private void seedBaselineDailySales(List<Store> stores, List<Sku> skus) {
        int totalRecords = stores.size() * skus.size() * BASELINE_DAYS;
        log.info("Seeding {} baseline daily sales records...", totalRecords);

        List<BaselineDailySales> allSales = new ArrayList<>(totalRecords);

        for (int storeIdx = 0; storeIdx < stores.size(); storeIdx++) {
            Store store = stores.get(storeIdx);

            for (int skuIdx = 0; skuIdx < skus.size(); skuIdx++) {
                Sku sku = skus.get(skuIdx);

                for (int dayOffset = 0; dayOffset < BASELINE_DAYS; dayOffset++) {
                    LocalDate salesDate = BASELINE_START_DATE.plusDays(dayOffset);
                    UUID stableId = generateStableUUID(SALES_NAMESPACE,
                        "sales-" + storeIdx + "-" + skuIdx + "-" + dayOffset);

                    // Deterministic units formula:
                    // baselineUnits = 80 + (skuIndex * 5) + (dayOffset % 7) * 2
                    // This creates a weekly pattern (day of week variation)
                    int units = 80 + (skuIdx * 5) + (dayOffset % 7) * 2;
                    BigDecimal unitsSold = BigDecimal.valueOf(units);

                    // Calculate revenue = price × units
                    // Use the base price for this store-sku
                    BigDecimal price = BigDecimal.valueOf(20 + (skuIdx * 3) + (storeIdx * 2));
                    BigDecimal revenue = price.multiply(unitsSold).setScale(2, RoundingMode.HALF_UP);

                    BaselineDailySales sales = new BaselineDailySales(
                        stableId, store, sku, salesDate, unitsSold);
                    sales.setRevenue(revenue);

                    allSales.add(sales);
                }
            }
        }

        // Batch save for performance
        baselineSalesRepository.saveAll(allSales);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Generates a deterministic UUID based on a namespace and name.
     * This ensures the same inputs always produce the same UUID.
     *
     * @param namespace the UUID namespace
     * @param name the unique name within the namespace
     * @return a deterministic UUID
     */
    private UUID generateStableUUID(UUID namespace, String name) {
        // Combine namespace and name for deterministic UUID generation
        String combined = namespace.toString() + ":" + name;
        return UUID.nameUUIDFromBytes(combined.getBytes());
    }

    // ========================================================================
    // PUBLIC ACCESSORS (for testing)
    // ========================================================================

    /**
     * Returns the number of stores that will be seeded.
     */
    public static int getStoreCount() {
        return STORE_DATA.length;
    }

    /**
     * Returns the number of SKUs that will be seeded.
     */
    public static int getSkuCount() {
        return SKU_DATA.length;
    }

    /**
     * Returns the number of days for baseline sales.
     */
    public static int getBaselineDays() {
        return BASELINE_DAYS;
    }

    /**
     * Calculates the expected base price for a given store and SKU index.
     * Formula: 20 + (skuIndex * 3) + (storeIndex * 2)
     */
    public static BigDecimal calculateExpectedPrice(int storeIndex, int skuIndex) {
        return BigDecimal.valueOf(20 + (skuIndex * 3) + (storeIndex * 2))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the expected cost for a given SKU index.
     * Formula: (20 + (skuIndex * 3) + 4) * 0.6
     */
    public static BigDecimal calculateExpectedCost(int skuIndex) {
        BigDecimal avgPrice = BigDecimal.valueOf(20 + (skuIndex * 3) + 4);
        return avgPrice.multiply(BigDecimal.valueOf(0.6))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the expected baseline units for a given SKU and day offset.
     * Formula: 80 + (skuIndex * 5) + (dayOffset % 7) * 2
     */
    public static int calculateExpectedUnits(int skuIndex, int dayOffset) {
        return 80 + (skuIndex * 5) + (dayOffset % 7) * 2;
    }
}
