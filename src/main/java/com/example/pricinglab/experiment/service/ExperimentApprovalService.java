package com.example.pricinglab.experiment.service;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.exception.InvalidStateTransitionException;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.dto.ApprovalRequest;
import com.example.pricinglab.experiment.repository.ExperimentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for experiment approval workflow.
 *
 * Only ADMIN users can approve or reject experiments.
 * Approval moves experiment from PENDING_APPROVAL to APPROVED.
 * Rejection moves experiment from PENDING_APPROVAL to REJECTED (terminal).
 */
@Service
@Transactional
public class ExperimentApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentApprovalService.class);

    private final ExperimentRepository experimentRepository;

    public ExperimentApprovalService(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    /**
     * Approves an experiment.
     * Requires ADMIN role.
     *
     * @param experimentId the experiment ID
     * @return the approved experiment
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Experiment approveExperiment(UUID experimentId) {
        log.info("Approving experiment: {}", experimentId);

        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        if (experiment.getStatus() != ExperimentStatus.PENDING_APPROVAL) {
            throw new InvalidStateTransitionException(experiment.getStatus(), ExperimentStatus.APPROVED);
        }

        // TODO: Perform final validation of guardrails
        // TODO: Check for overlapping experiments on same store-SKU combinations
        // TODO: Create audit log entry

        String approver = SecurityContextHolder.getContext().getAuthentication().getName();
        experiment.setApprovedBy(approver);
        experiment.setStatus(ExperimentStatus.APPROVED);

        return experimentRepository.save(experiment);
    }

    /**
     * Rejects an experiment.
     * Requires ADMIN role.
     *
     * @param experimentId the experiment ID
     * @param request the rejection request with reason
     * @return the rejected experiment
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Experiment rejectExperiment(UUID experimentId, ApprovalRequest request) {
        log.info("Rejecting experiment: {}", experimentId);

        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        if (experiment.getStatus() != ExperimentStatus.PENDING_APPROVAL) {
            throw new InvalidStateTransitionException(experiment.getStatus(), ExperimentStatus.REJECTED);
        }

        // TODO: Create audit log entry

        String approver = SecurityContextHolder.getContext().getAuthentication().getName();
        experiment.setApprovedBy(approver);
        experiment.setRejectionReason(request.rejectionReason());
        experiment.setStatus(ExperimentStatus.REJECTED);

        return experimentRepository.save(experiment);
    }

    /**
     * Processes an approval or rejection request.
     * Requires ADMIN role.
     *
     * @param experimentId the experiment ID
     * @param request the approval request
     * @return the updated experiment
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Experiment processApproval(UUID experimentId, ApprovalRequest request) {
        if (request.approved()) {
            return approveExperiment(experimentId);
        } else {
            return rejectExperiment(experimentId, request);
        }
    }

    /**
     * Lists experiments pending approval.
     *
     * @return list of experiments awaiting approval
     */
    @Transactional(readOnly = true)
    public List<Experiment> listPendingApprovals() {
        return experimentRepository.findByStatus(ExperimentStatus.PENDING_APPROVAL);
    }
}
