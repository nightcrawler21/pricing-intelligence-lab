package com.example.pricinglab.simulation.repository;

import com.example.pricinglab.simulation.domain.SimulationResultDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SimulationResultDaily entities.
 */
@Repository
public interface SimulationResultDailyRepository extends JpaRepository<SimulationResultDaily, UUID> {

    List<SimulationResultDaily> findBySimulationRunId(UUID simulationRunId);

    List<SimulationResultDaily> findBySimulationRunIdAndTestGroup(UUID simulationRunId, boolean testGroup);

    List<SimulationResultDaily> findBySimulationRunIdAndSimulationDate(UUID simulationRunId, LocalDate date);

    @Query("""
        SELECT srd FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        AND srd.store.id = :storeId
        ORDER BY srd.simulationDate
        """)
    List<SimulationResultDaily> findByRunAndStore(
        @Param("runId") UUID runId,
        @Param("storeId") UUID storeId
    );

    @Query("""
        SELECT srd FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        AND srd.sku.id = :skuId
        ORDER BY srd.simulationDate
        """)
    List<SimulationResultDaily> findByRunAndSku(
        @Param("runId") UUID runId,
        @Param("skuId") UUID skuId
    );

    // --- Aggregate Query Projections ---

    /**
     * Projection for variant aggregate totals.
     */
    interface VariantAggregate {
        boolean isTestGroup();
        BigDecimal getTotalUnits();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalMargin();
    }

    /**
     * Projection for timeseries aggregate by date.
     */
    interface DateVariantAggregate {
        LocalDate getSimulationDate();
        boolean isTestGroup();
        BigDecimal getTotalUnits();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalMargin();
    }

    /**
     * Projection for breakdown aggregate by store.
     */
    interface StoreVariantAggregate {
        UUID getStoreId();
        boolean isTestGroup();
        BigDecimal getTotalUnits();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalMargin();
    }

    /**
     * Projection for breakdown aggregate by SKU.
     */
    interface SkuVariantAggregate {
        UUID getSkuId();
        boolean isTestGroup();
        BigDecimal getTotalUnits();
        BigDecimal getTotalRevenue();
        BigDecimal getTotalMargin();
    }

    // --- Aggregate Queries ---

    /**
     * Aggregates totals by variant (CONTROL/TEST) for a simulation run.
     */
    @Query("""
        SELECT srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        GROUP BY srd.testGroup
        """)
    List<VariantAggregate> aggregateTotalsByVariant(@Param("runId") UUID runId);

    /**
     * Aggregates totals by date and variant for timeseries data.
     */
    @Query("""
        SELECT srd.simulationDate AS simulationDate,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        GROUP BY srd.simulationDate, srd.testGroup
        ORDER BY srd.simulationDate ASC, srd.testGroup ASC
        """)
    List<DateVariantAggregate> aggregateByDateAndVariant(@Param("runId") UUID runId);

    /**
     * Aggregates totals by date and variant with store filter.
     */
    @Query("""
        SELECT srd.simulationDate AS simulationDate,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
          AND srd.store.id = :storeId
        GROUP BY srd.simulationDate, srd.testGroup
        ORDER BY srd.simulationDate ASC, srd.testGroup ASC
        """)
    List<DateVariantAggregate> aggregateByDateAndVariantForStore(
            @Param("runId") UUID runId,
            @Param("storeId") UUID storeId
    );

    /**
     * Aggregates totals by date and variant with SKU filter.
     */
    @Query("""
        SELECT srd.simulationDate AS simulationDate,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
          AND srd.sku.id = :skuId
        GROUP BY srd.simulationDate, srd.testGroup
        ORDER BY srd.simulationDate ASC, srd.testGroup ASC
        """)
    List<DateVariantAggregate> aggregateByDateAndVariantForSku(
            @Param("runId") UUID runId,
            @Param("skuId") UUID skuId
    );

    /**
     * Aggregates totals by date and variant with both store and SKU filter.
     */
    @Query("""
        SELECT srd.simulationDate AS simulationDate,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
          AND srd.store.id = :storeId
          AND srd.sku.id = :skuId
        GROUP BY srd.simulationDate, srd.testGroup
        ORDER BY srd.simulationDate ASC, srd.testGroup ASC
        """)
    List<DateVariantAggregate> aggregateByDateAndVariantForStoreAndSku(
            @Param("runId") UUID runId,
            @Param("storeId") UUID storeId,
            @Param("skuId") UUID skuId
    );

    /**
     * Aggregates totals by store and variant for breakdown.
     */
    @Query("""
        SELECT srd.store.id AS storeId,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        GROUP BY srd.store.id, srd.testGroup
        ORDER BY srd.store.id ASC, srd.testGroup ASC
        """)
    List<StoreVariantAggregate> aggregateByStoreAndVariant(@Param("runId") UUID runId);

    /**
     * Aggregates totals by store and variant with SKU filter.
     */
    @Query("""
        SELECT srd.store.id AS storeId,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
          AND srd.sku.id = :skuId
        GROUP BY srd.store.id, srd.testGroup
        ORDER BY srd.store.id ASC, srd.testGroup ASC
        """)
    List<StoreVariantAggregate> aggregateByStoreAndVariantForSku(
            @Param("runId") UUID runId,
            @Param("skuId") UUID skuId
    );

    /**
     * Aggregates totals by SKU and variant for breakdown.
     */
    @Query("""
        SELECT srd.sku.id AS skuId,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        GROUP BY srd.sku.id, srd.testGroup
        ORDER BY srd.sku.id ASC, srd.testGroup ASC
        """)
    List<SkuVariantAggregate> aggregateBySkuAndVariant(@Param("runId") UUID runId);

    /**
     * Aggregates totals by SKU and variant with store filter.
     */
    @Query("""
        SELECT srd.sku.id AS skuId,
               srd.testGroup AS testGroup,
               SUM(srd.projectedUnits) AS totalUnits,
               SUM(srd.projectedRevenue) AS totalRevenue,
               SUM(srd.projectedMargin) AS totalMargin
        FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
          AND srd.store.id = :storeId
        GROUP BY srd.sku.id, srd.testGroup
        ORDER BY srd.sku.id ASC, srd.testGroup ASC
        """)
    List<SkuVariantAggregate> aggregateBySkuAndVariantForStore(
            @Param("runId") UUID runId,
            @Param("storeId") UUID storeId
    );

    /**
     * Finds all results for a run ordered for CSV export.
     */
    @Query("""
        SELECT srd FROM SimulationResultDaily srd
        WHERE srd.simulationRun.id = :runId
        ORDER BY srd.simulationDate ASC, srd.store.id ASC, srd.sku.id ASC, srd.testGroup ASC
        """)
    List<SimulationResultDaily> findByRunIdOrderedForExport(@Param("runId") UUID runId);
}
