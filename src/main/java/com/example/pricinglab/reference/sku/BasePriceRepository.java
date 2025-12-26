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
}
