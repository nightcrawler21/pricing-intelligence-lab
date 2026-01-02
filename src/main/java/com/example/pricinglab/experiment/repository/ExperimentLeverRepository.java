package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.experiment.domain.ExperimentLever;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ExperimentLever entities.
 */
@Repository
public interface ExperimentLeverRepository extends JpaRepository<ExperimentLever, UUID> {

    List<ExperimentLever> findByExperimentId(UUID experimentId);

    /**
     * Finds the single lever for an experiment (v0: one lever per experiment).
     */
    Optional<ExperimentLever> findFirstByExperimentId(UUID experimentId);

    /**
     * Checks if a lever exists for the given experiment.
     */
    boolean existsByExperimentId(UUID experimentId);

    /**
     * Deletes the lever for a given experiment.
     */
    void deleteByExperimentId(UUID experimentId);

    List<ExperimentLever> findBySkuId(UUID skuId);
}
