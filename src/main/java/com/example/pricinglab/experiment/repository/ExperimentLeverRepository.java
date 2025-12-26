package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.experiment.domain.ExperimentLever;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ExperimentLever entities.
 */
@Repository
public interface ExperimentLeverRepository extends JpaRepository<ExperimentLever, UUID> {

    List<ExperimentLever> findByExperimentId(UUID experimentId);

    List<ExperimentLever> findBySkuId(UUID skuId);
}
