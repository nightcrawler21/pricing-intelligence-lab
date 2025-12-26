package com.example.pricinglab.simulation.service;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.common.exception.InvalidStateTransitionException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.simulation.domain.SimulationRun;
import com.example.pricinglab.simulation.repository.SimulationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for running pricing simulations.
 *
 * Simulations project the expected outcomes of a pricing experiment
 * by applying the defined levers to historical sales data.
 *
 * This is a SIMULATION ONLY tool - no actual pricing changes are made.
 */
@Service
@Transactional
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulationRunRepository simulationRunRepository;
    private final ExperimentRepository experimentRepository;

    public SimulationService(
        SimulationRunRepository simulationRunRepository,
        ExperimentRepository experimentRepository
    ) {
        this.simulationRunRepository = simulationRunRepository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * Starts a new simulation for an approved experiment.
     *
     * @param experimentId the experiment ID
     * @return the created simulation run
     */
    public SimulationRun startSimulation(UUID experimentId) {
        log.info("Starting simulation for experiment: {}", experimentId);

        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        if (experiment.getStatus() != ExperimentStatus.APPROVED) {
            throw new InvalidStateTransitionException(experiment.getStatus(), ExperimentStatus.RUNNING);
        }

        // Update experiment status
        experiment.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(experiment);

        // Create simulation run
        SimulationRun run = new SimulationRun(UUID.randomUUID(), experiment);
        run.setStatus(SimulationStatus.RUNNING);
        run.setStartedAt(Instant.now());

        run = simulationRunRepository.save(run);

        // TODO: Trigger actual simulation logic asynchronously
        // For v0, we'll just create the run record
        // In future versions, this would queue the simulation for background processing

        log.info("Simulation run {} created for experiment {}", run.getId(), experimentId);

        return run;
    }

    /**
     * Retrieves a simulation run by ID.
     *
     * @param runId the simulation run ID
     * @return the simulation run
     */
    @Transactional(readOnly = true)
    public SimulationRun getSimulationRun(UUID runId) {
        return simulationRunRepository.findById(runId)
            .orElseThrow(() -> new ResourceNotFoundException("SimulationRun", runId.toString()));
    }

    /**
     * Lists all simulation runs for an experiment.
     *
     * @param experimentId the experiment ID
     * @return list of simulation runs
     */
    @Transactional(readOnly = true)
    public List<SimulationRun> listSimulationRuns(UUID experimentId) {
        return simulationRunRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    /**
     * Completes a simulation run with results.
     * This would be called by the simulation engine after processing.
     *
     * @param runId the simulation run ID
     */
    public void completeSimulation(UUID runId) {
        // TODO: Implement simulation completion logic

        SimulationRun run = getSimulationRun(runId);
        run.setStatus(SimulationStatus.COMPLETED);
        run.setCompletedAt(Instant.now());

        // TODO: Calculate summary metrics from daily results
        // TODO: Update experiment status to COMPLETED if this is the final run

        simulationRunRepository.save(run);
        log.info("Simulation run {} completed", runId);
    }

    /**
     * Marks a simulation run as failed.
     *
     * @param runId the simulation run ID
     * @param errorMessage the error message
     */
    public void failSimulation(UUID runId, String errorMessage) {
        SimulationRun run = getSimulationRun(runId);
        run.setStatus(SimulationStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(errorMessage);

        simulationRunRepository.save(run);
        log.error("Simulation run {} failed: {}", runId, errorMessage);
    }

    /**
     * Calculates projected metrics for a simulation.
     * Stub for v0 - actual implementation would use historical data and price elasticity models.
     *
     * @param run the simulation run
     */
    public void calculateProjections(SimulationRun run) {
        // TODO: Implement projection calculation logic
        // This would involve:
        // 1. Loading historical sales data for the experiment period
        // 2. Applying levers to calculate test prices
        // 3. Using price elasticity models to project demand changes
        // 4. Calculating revenue and margin impacts
        // 5. Storing daily results

        log.info("Projection calculation not yet implemented for run {}", run.getId());
    }
}
