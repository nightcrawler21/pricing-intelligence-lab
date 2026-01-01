package com.example.pricinglab.experiment.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.dto.CreateExperimentRequest;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing pricing experiments.
 *
 * Handles experiment lifecycle operations including creation, updates,
 * and state transitions.
 */
@Service
@Transactional
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    private final ExperimentRepository experimentRepository;
    private final ExperimentLifecycleValidator lifecycleValidator;
    private final AuditService auditService;

    public ExperimentService(
            ExperimentRepository experimentRepository,
            ExperimentLifecycleValidator lifecycleValidator,
            AuditService auditService) {
        this.experimentRepository = experimentRepository;
        this.lifecycleValidator = lifecycleValidator;
        this.auditService = auditService;
    }

    /**
     * Creates a new experiment in DRAFT status.
     *
     * @param request the experiment creation request
     * @return the created experiment
     */
    public Experiment createExperiment(CreateExperimentRequest request) {
        log.info("Creating new experiment: {}", request.name());

        // TODO: Validate that end date is after start date
        // TODO: Validate experiment name uniqueness (optional)

        Experiment experiment = new Experiment(
            UUID.randomUUID(),
            request.name(),
            request.startDate(),
            request.endDate()
        );
        experiment.setDescription(request.description());
        experiment.setBusinessJustification(request.businessJustification());
        experiment.setHypothesis(request.hypothesis());
        experiment.setStatus(ExperimentStatus.DRAFT);

        return experimentRepository.save(experiment);
    }

    /**
     * Retrieves an experiment by ID.
     *
     * @param id the experiment ID
     * @return the experiment
     * @throws ResourceNotFoundException if experiment not found
     */
    @Transactional(readOnly = true)
    public Experiment getExperiment(UUID id) {
        return experimentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", id.toString()));
    }

    /**
     * Lists all experiments.
     *
     * @return list of all experiments
     */
    @Transactional(readOnly = true)
    public List<Experiment> listExperiments() {
        // TODO: Add pagination support
        // TODO: Add filtering by status, date range, created by
        return experimentRepository.findAll();
    }

    /**
     * Lists experiments by status.
     *
     * @param status the status to filter by
     * @return list of experiments with given status
     */
    @Transactional(readOnly = true)
    public List<Experiment> listExperimentsByStatus(ExperimentStatus status) {
        return experimentRepository.findByStatus(status);
    }

    /**
     * Submits a DRAFT experiment for approval.
     *
     * @param id the experiment ID
     * @return the updated experiment
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public Experiment submitForApproval(UUID id) {
        Experiment experiment = getExperiment(id);

        lifecycleValidator.validateCanSubmit(experiment);

        // TODO: Validate experiment has required scope (at least one store-SKU pair)
        // TODO: Validate experiment has at least one lever defined
        // TODO: Validate experiment has guardrails configured
        // TODO: Validate all levers comply with guardrails

        ExperimentStatus previousStatus = experiment.getStatus();
        experiment.setStatus(ExperimentStatus.PENDING_APPROVAL);
        Experiment saved = experimentRepository.save(experiment);

        log.info("Experiment {} submitted for approval (status: {} → {})",
                id, previousStatus, ExperimentStatus.PENDING_APPROVAL);

        auditService.logAction(
                "Experiment",
                id,
                AuditAction.EXPERIMENT_SUBMITTED,
                String.format("Status changed from %s to %s", previousStatus, ExperimentStatus.PENDING_APPROVAL)
        );

        return saved;
    }

    /**
     * Updates an experiment's basic information.
     * Only allowed when experiment is in DRAFT status.
     *
     * @param id the experiment ID
     * @param request the update request
     * @return the updated experiment
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public Experiment updateExperiment(UUID id, CreateExperimentRequest request) {
        Experiment experiment = getExperiment(id);

        if (experiment.getStatus() != ExperimentStatus.DRAFT) {
            throw new InvalidExperimentStateException(
                    experiment.getStatus(),
                    "update",
                    ExperimentStatus.DRAFT
            );
        }

        // TODO: Validate end date is after start date

        experiment.setName(request.name());
        experiment.setDescription(request.description());
        experiment.setStartDate(request.startDate());
        experiment.setEndDate(request.endDate());
        experiment.setBusinessJustification(request.businessJustification());
        experiment.setHypothesis(request.hypothesis());

        Experiment saved = experimentRepository.save(experiment);

        auditService.logAction(
                "Experiment",
                id,
                AuditAction.EXPERIMENT_UPDATED,
                "Experiment details updated"
        );

        return saved;
    }
}
