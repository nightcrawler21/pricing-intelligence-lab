package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.experiment.domain.Experiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Experiment entities.
 */
@Repository
public interface ExperimentRepository extends JpaRepository<Experiment, UUID> {

    List<Experiment> findByStatus(ExperimentStatus status);

    List<Experiment> findByCreatedBy(String createdBy);

    List<Experiment> findByStatusIn(List<ExperimentStatus> statuses);

    List<Experiment> findByNameContainingIgnoreCase(String name);
}
