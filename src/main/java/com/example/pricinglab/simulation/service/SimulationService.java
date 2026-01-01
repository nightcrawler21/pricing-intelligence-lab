package com.example.pricinglab.simulation.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import com.example.pricinglab.experiment.service.ExperimentLifecycleValidator;
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
    private final ExperimentLifecycleValidator lifecycleValidator;
    private final AuditService auditService;

    public SimulationService(
            SimulationRunRepository simulationRunRepository,
            ExperimentRepository experimentRepository,
            ExperimentLifecycleValidator lifecycleValidator,
            AuditService auditService) {
        this.simulationRunRepository = simulationRunRepository;
        this.experimentRepository = experimentRepository;
        this.lifecycleValidator = lifecycleValidator;
        this.auditService = auditService;
    }

    /**
     * Starts a new simulation for an approved experiment.
     *
     * @param experimentId the experiment ID
     * @return the created simulation run
     */
    public SimulationRun startSimulation(UUID experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        lifecycleValidator.validateCanStartSimulation(experiment);

        ExperimentStatus previousStatus = experiment.getStatus();

        // Update experiment status
        experiment.setStatus(ExperimentStatus.RUNNING);
        experimentRepository.save(experiment);

        // Create simulation run
        SimulationRun run = new SimulationRun(UUID.randomUUID(), experiment);
        run.setStatus(SimulationStatus.RUNNING);
        run.setStartedAt(Instant.now());

        run = simulationRunRepository.save(run);

        log.info("Simulation run {} started for experiment {} (status: {} → {})",
                run.getId(), experimentId, previousStatus, ExperimentStatus.RUNNING);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.SIMULATION_STARTED,
                String.format("Simulation run %s started. Status changed from %s to %s",
                        run.getId(), previousStatus, ExperimentStatus.RUNNING)
        );

        // TODO: Trigger actual simulation logic asynchronously
        // For v0, we'll just create the run record
        // In future versions, this would queue the simulation for background processing

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
     * Also transitions the experiment from RUNNING to COMPLETED.
     *
     * @param runId the simulation run ID
     */
    public void completeSimulation(UUID runId) {
        SimulationRun run = getSimulationRun(runId);
        Experiment experiment = run.getExperiment();

        lifecycleValidator.validateCanComplete(experiment);

        ExperimentStatus previousStatus = experiment.getStatus();

        // Update simulation run
        run.setStatus(SimulationStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        simulationRunRepository.save(run);

        // Update experiment status
        experiment.setStatus(ExperimentStatus.COMPLETED);
        experimentRepository.save(experiment);

        log.info("Simulation run {} completed. Experiment {} status: {} → {}",
                runId, experiment.getId(), previousStatus, ExperimentStatus.COMPLETED);

        auditService.logAction(
                "Experiment",
                experiment.getId(),
                AuditAction.SIMULATION_COMPLETED,
                String.format("Simulation run %s completed. Status changed from %s to %s",
                        runId, previousStatus, ExperimentStatus.COMPLETED)
        );

        // TODO: Calculate summary metrics from daily results
    }

    /**
     * Marks a simulation run as failed.
     * Also transitions the experiment from RUNNING to FAILED.
     *
     * @param runId the simulation run ID
     * @param errorMessage the error message
     */
    public void failSimulation(UUID runId, String errorMessage) {
        SimulationRun run = getSimulationRun(runId);
        Experiment experiment = run.getExperiment();

        lifecycleValidator.validateCanFail(experiment);

        ExperimentStatus previousStatus = experiment.getStatus();

        // Update simulation run
        run.setStatus(SimulationStatus.FAILED);
        run.setCompletedAt(Instant.now());
        run.setErrorMessage(errorMessage);
        simulationRunRepository.save(run);

        // Update experiment status
        experiment.setStatus(ExperimentStatus.FAILED);
        experimentRepository.save(experiment);

        log.error("Simulation run {} failed: {}. Experiment {} status: {} → {}",
                runId, errorMessage, experiment.getId(), previousStatus, ExperimentStatus.FAILED);

        auditService.logAction(
                "Experiment",
                experiment.getId(),
                AuditAction.SIMULATION_FAILED,
                String.format("Simulation run %s failed: %s. Status changed from %s to %s",
                        runId, errorMessage, previousStatus, ExperimentStatus.FAILED)
        );
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
