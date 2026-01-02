package com.example.pricinglab.reference.sku;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SkuCost entities.
 */
@Repository
public interface SkuCostRepository extends JpaRepository<SkuCost, UUID> {

    List<SkuCost> findBySkuId(UUID skuId);

    /**
     * Find the effective cost for a SKU on a given date.
     */
    @Query("""
        SELECT sc FROM SkuCost sc
        WHERE sc.sku.id = :skuId
        AND sc.effectiveDate <= :date
        AND (sc.endDate IS NULL OR sc.endDate >= :date)
        ORDER BY sc.effectiveDate DESC
        LIMIT 1
        """)
    Optional<SkuCost> findEffectiveCost(
        @Param("skuId") UUID skuId,
        @Param("date") LocalDate date
    );

    /**
     * Count SKU costs for SKUs with code starting with given prefix.
     * Used for testing seeded data isolation.
     */
    @Query("SELECT COUNT(sc) FROM SkuCost sc WHERE sc.sku.skuCode LIKE CONCAT(:prefix, '%')")
    long countBySkuCodeStartingWith(@Param("prefix") String prefix);
}
