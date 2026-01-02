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
 * Repository for BasePrice entities.
 */
@Repository
public interface BasePriceRepository extends JpaRepository<BasePrice, UUID> {

    List<BasePrice> findBySkuId(UUID skuId);

    List<BasePrice> findByStoreId(UUID storeId);

    /**
     * Find the effective base price for a SKU at a store on a given date.
     */
    @Query("""
        SELECT bp FROM BasePrice bp
        WHERE bp.sku.id = :skuId
        AND bp.store.id = :storeId
        AND bp.effectiveDate <= :date
        AND (bp.endDate IS NULL OR bp.endDate >= :date)
        ORDER BY bp.effectiveDate DESC
        LIMIT 1
        """)
    Optional<BasePrice> findEffectivePrice(
        @Param("skuId") UUID skuId,
        @Param("storeId") UUID storeId,
        @Param("date") LocalDate date
    );

    /**
     * Find all effective base prices for a SKU across all stores on a given date.
     * Used for guardrail validation to find the minimum base price (most conservative).
     */
    @Query("""
        SELECT bp FROM BasePrice bp
        WHERE bp.sku.id = :skuId
        AND bp.effectiveDate <= :date
        AND (bp.endDate IS NULL OR bp.endDate >= :date)
        """)
    List<BasePrice> findAllEffectivePricesForSku(
        @Param("skuId") UUID skuId,
        @Param("date") LocalDate date
    );

    /**
     * Count base prices for SKUs with code starting with given prefix.
     * Used for testing seeded data isolation.
     */
    @Query("SELECT COUNT(bp) FROM BasePrice bp WHERE bp.sku.skuCode LIKE CONCAT(:prefix, '%')")
    long countBySkuCodeStartingWith(@Param("prefix") String prefix);
}
