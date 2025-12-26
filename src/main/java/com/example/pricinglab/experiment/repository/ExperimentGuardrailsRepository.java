package com.example.pricinglab.experiment.repository;

import com.example.pricinglab.experiment.domain.ExperimentGuardrails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ExperimentGuardrails entities.
 */
@Repository
public interface ExperimentGuardrailsRepository extends JpaRepository<ExperimentGuardrails, UUID> {

    Optional<ExperimentGuardrails> findByExperimentId(UUID experimentId);
}
