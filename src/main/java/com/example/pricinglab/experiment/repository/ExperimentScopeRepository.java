package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.experiment.domain.ExperimentScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ExperimentScope entities.
 */
@Repository
public interface ExperimentScopeRepository extends JpaRepository<ExperimentScope, UUID> {

    List<ExperimentScope> findByExperimentId(UUID experimentId);

    List<ExperimentScope> findByExperimentIdAndTestGroup(UUID experimentId, boolean testGroup);

    List<ExperimentScope> findByStoreId(UUID storeId);

    List<ExperimentScope> findBySkuId(UUID skuId);

    /**
     * Checks if a scope entry exists for the given experiment, store, and SKU combination.
     */
    boolean existsByExperimentIdAndStoreIdAndSkuId(UUID experimentId, UUID storeId, UUID skuId);

    /**
     * Finds a scope entry by experiment, store, and SKU combination.
     */
    Optional<ExperimentScope> findByExperimentIdAndStoreIdAndSkuId(
        UUID experimentId, UUID storeId, UUID skuId);

    /**
     * Deletes scope entries by experiment, store, and SKU combination.
     * Returns the number of deleted entries.
     */
    @Query("DELETE FROM ExperimentScope s WHERE s.experiment.id = :experimentId " +
           "AND s.store.id = :storeId AND s.sku.id = :skuId")
    int deleteByExperimentIdAndStoreIdAndSkuId(
        @Param("experimentId") UUID experimentId,
        @Param("storeId") UUID storeId,
        @Param("skuId") UUID skuId);

    /**
     * Counts scope entries for an experiment.
     */
    int countByExperimentId(UUID experimentId);
}
