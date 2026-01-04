package com.example.pricinglab.experiment.service;

import com.example.pricinglab.audit.AuditService;
import com.example.pricinglab.common.enums.AuditAction;
import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.exception.ResourceNotFoundException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentGuardrails;
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
    private final ExperimentLifecycleValidator lifecycleValidator;
    private final AuditService auditService;

    public ExperimentApprovalService(
            ExperimentRepository experimentRepository,
            ExperimentLifecycleValidator lifecycleValidator,
            AuditService auditService) {
        this.experimentRepository = experimentRepository;
        this.lifecycleValidator = lifecycleValidator;
        this.auditService = auditService;
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
        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        lifecycleValidator.validateCanApprove(experiment);

        // TODO: Perform final validation of guardrails
        // TODO: Check for overlapping experiments on same store-SKU combinations

        ExperimentStatus previousStatus = experiment.getStatus();
        String approver = SecurityContextHolder.getContext().getAuthentication().getName();
        experiment.setApprovedBy(approver);
        experiment.setStatus(ExperimentStatus.APPROVED);

        Experiment saved = experimentRepository.save(experiment);

        log.info("Experiment {} approved by {} (status: {} → {})",
                experimentId, approver, previousStatus, ExperimentStatus.APPROVED);

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_APPROVED,
                String.format("Approved by %s. Status changed from %s to %s",
                        approver, previousStatus, ExperimentStatus.APPROVED)
        );

        // Initialize lazy collections before returning (needed for DTO mapping outside transaction)
        initializeLazyCollections(saved);

        return saved;
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
        Experiment experiment = experimentRepository.findById(experimentId)
            .orElseThrow(() -> new ResourceNotFoundException("Experiment", experimentId.toString()));

        lifecycleValidator.validateCanReject(experiment);

        ExperimentStatus previousStatus = experiment.getStatus();
        String approver = SecurityContextHolder.getContext().getAuthentication().getName();
        experiment.setApprovedBy(approver);
        experiment.setRejectionReason(request.rejectionReason());
        experiment.setStatus(ExperimentStatus.REJECTED);

        Experiment saved = experimentRepository.save(experiment);

        log.info("Experiment {} rejected by {} (status: {} → {}). Reason: {}",
                experimentId, approver, previousStatus, ExperimentStatus.REJECTED, request.rejectionReason());

        auditService.logAction(
                "Experiment",
                experimentId,
                AuditAction.EXPERIMENT_REJECTED,
                String.format("Rejected by %s. Reason: %s. Status changed from %s to %s",
                        approver, request.rejectionReason(), previousStatus, ExperimentStatus.REJECTED)
        );

        // Initialize lazy collections before returning (needed for DTO mapping outside transaction)
        initializeLazyCollections(saved);

        return saved;
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

    /**
     * Initialize lazy collections to avoid LazyInitializationException when mapping to DTO.
     */
    private void initializeLazyCollections(Experiment experiment) {
        experiment.getScopes().size();
        experiment.getLevers().size();
        ExperimentGuardrails g = experiment.getGuardrails();
        if (g != null) {
            g.getId();
        }
    }
}
