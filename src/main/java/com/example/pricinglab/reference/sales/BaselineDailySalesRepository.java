package com.example.pricinglab.reference.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BaselineDailySales entities.
 */
@Repository
public interface BaselineDailySalesRepository extends JpaRepository<BaselineDailySales, UUID> {

    /**
     * Find all baseline sales for a specific store-SKU combination.
     */
    List<BaselineDailySales> findByStoreIdAndSkuId(UUID storeId, UUID skuId);

    /**
     * Find baseline sales for a specific store-SKU combination within a date range.
     */
    @Query("""
        SELECT b FROM BaselineDailySales b
        WHERE b.store.id = :storeId
        AND b.sku.id = :skuId
        AND b.salesDate >= :startDate
        AND b.salesDate <= :endDate
        ORDER BY b.salesDate
        """)
    List<BaselineDailySales> findByStoreSkuAndDateRange(
        @Param("storeId") UUID storeId,
        @Param("skuId") UUID skuId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find a specific baseline record by store, SKU, and date.
     */
    Optional<BaselineDailySales> findByStoreIdAndSkuIdAndSalesDate(
        UUID storeId,
        UUID skuId,
        LocalDate salesDate
    );

    /**
     * Find all baseline sales for a store on a given date.
     */
    List<BaselineDailySales> findByStoreIdAndSalesDate(UUID storeId, LocalDate salesDate);

    /**
     * Find all baseline sales for a SKU on a given date (across all stores).
     */
    List<BaselineDailySales> findBySkuIdAndSalesDate(UUID skuId, LocalDate salesDate);

    /**
     * Count baseline records for a specific store-SKU combination.
     */
    long countByStoreIdAndSkuId(UUID storeId, UUID skuId);

    /**
     * Delete all baseline sales for a store (used in local seeding).
     */
    void deleteByStoreId(UUID storeId);

    /**
     * Count baseline sales for SKUs with code starting with given prefix.
     * Used for testing seeded data isolation.
     */
    @Query("SELECT COUNT(b) FROM BaselineDailySales b WHERE b.sku.skuCode LIKE CONCAT(:prefix, '%')")
    long countBySkuCodeStartingWith(@Param("prefix") String prefix);
}
