package com.example.pricinglab.simulation.repository;

import com.example.pricinglab.simulation.domain.SimulationResultDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
