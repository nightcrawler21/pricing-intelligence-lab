package com.example.pricinglab.simulation.repository;

import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.simulation.domain.SimulationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SimulationRun entities.
 */
@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {

    List<SimulationRun> findByExperimentId(UUID experimentId);

    List<SimulationRun> findByExperimentIdOrderByCreatedAtDesc(UUID experimentId);

    List<SimulationRun> findByStatus(SimulationStatus status);
}
