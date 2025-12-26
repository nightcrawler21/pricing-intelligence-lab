package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.experiment.domain.ExperimentScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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
}
