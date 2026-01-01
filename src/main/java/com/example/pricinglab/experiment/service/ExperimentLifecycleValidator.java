package com.example.pricinglab.experiment.service;

import com.example.pricinglab.common.enums.ExperimentStatus;
import com.example.pricinglab.common.exception.InvalidExperimentStateException;
import com.example.pricinglab.experiment.domain.Experiment;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized validator for experiment lifecycle state transitions.
 *
 * <p>Enforces the following allowed transitions:</p>
 * <ul>
 *   <li>DRAFT → PENDING_APPROVAL (submit)</li>
 *   <li>PENDING_APPROVAL → APPROVED (approve)</li>
 *   <li>PENDING_APPROVAL → REJECTED (reject)</li>
 *   <li>APPROVED → RUNNING (start simulation)</li>
 *   <li>RUNNING → COMPLETED (complete simulation)</li>
 *   <li>RUNNING → FAILED (fail simulation)</li>
 * </ul>
 *
 * <p>Terminal states (no further transitions allowed): COMPLETED, FAILED, REJECTED</p>
 */
@Component
public class ExperimentLifecycleValidator {

    /**
     * Map defining which states can transition to which other states.
     */
    private static final Map<ExperimentStatus, Set<ExperimentStatus>> ALLOWED_TRANSITIONS;

    /**
     * States that cannot be transitioned from (terminal states).
     */
    private static final Set<ExperimentStatus> TERMINAL_STATES = EnumSet.of(
            ExperimentStatus.COMPLETED,
            ExperimentStatus.FAILED,
            ExperimentStatus.REJECTED
    );

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(ExperimentStatus.class);

        // DRAFT can only transition to PENDING_APPROVAL
        ALLOWED_TRANSITIONS.put(ExperimentStatus.DRAFT,
                EnumSet.of(ExperimentStatus.PENDING_APPROVAL));

        // PENDING_APPROVAL can transition to APPROVED or REJECTED
        ALLOWED_TRANSITIONS.put(ExperimentStatus.PENDING_APPROVAL,
                EnumSet.of(ExperimentStatus.APPROVED, ExperimentStatus.REJECTED));

        // APPROVED can only transition to RUNNING
        ALLOWED_TRANSITIONS.put(ExperimentStatus.APPROVED,
                EnumSet.of(ExperimentStatus.RUNNING));

        // RUNNING can transition to COMPLETED or FAILED
        ALLOWED_TRANSITIONS.put(ExperimentStatus.RUNNING,
                EnumSet.of(ExperimentStatus.COMPLETED, ExperimentStatus.FAILED));

        // Terminal states have no allowed transitions
        ALLOWED_TRANSITIONS.put(ExperimentStatus.COMPLETED, EnumSet.noneOf(ExperimentStatus.class));
        ALLOWED_TRANSITIONS.put(ExperimentStatus.FAILED, EnumSet.noneOf(ExperimentStatus.class));
        ALLOWED_TRANSITIONS.put(ExperimentStatus.REJECTED, EnumSet.noneOf(ExperimentStatus.class));
    }

    /**
     * Validates that the experiment can be submitted for approval.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in DRAFT status
     */
    public void validateCanSubmit(Experiment experiment) {
        validateCurrentState(experiment, "submit", ExperimentStatus.DRAFT);
    }

    /**
     * Validates that the experiment can be approved.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in PENDING_APPROVAL status
     */
    public void validateCanApprove(Experiment experiment) {
        validateCurrentState(experiment, "approve", ExperimentStatus.PENDING_APPROVAL);
    }

    /**
     * Validates that the experiment can be rejected.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in PENDING_APPROVAL status
     */
    public void validateCanReject(Experiment experiment) {
        validateCurrentState(experiment, "reject", ExperimentStatus.PENDING_APPROVAL);
    }

    /**
     * Validates that the experiment can start a simulation.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in APPROVED status
     */
    public void validateCanStartSimulation(Experiment experiment) {
        validateCurrentState(experiment, "start simulation", ExperimentStatus.APPROVED);
    }

    /**
     * Validates that the experiment can be marked as completed.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in RUNNING status
     */
    public void validateCanComplete(Experiment experiment) {
        validateCurrentState(experiment, "complete simulation", ExperimentStatus.RUNNING);
    }

    /**
     * Validates that the experiment can be marked as failed.
     *
     * @param experiment the experiment to validate
     * @throws InvalidExperimentStateException if experiment is not in RUNNING status
     */
    public void validateCanFail(Experiment experiment) {
        validateCurrentState(experiment, "fail simulation", ExperimentStatus.RUNNING);
    }

    /**
     * Validates that an experiment is in one of the allowed states for an action.
     *
     * @param experiment the experiment to validate
     * @param action     the action being attempted
     * @param allowedStatuses the statuses that allow this action
     * @throws InvalidExperimentStateException if experiment is not in an allowed status
     */
    private void validateCurrentState(
            Experiment experiment,
            String action,
            ExperimentStatus... allowedStatuses) {

        ExperimentStatus currentStatus = experiment.getStatus();
        for (ExperimentStatus allowed : allowedStatuses) {
            if (currentStatus == allowed) {
                return; // Valid state
            }
        }

        throw new InvalidExperimentStateException(currentStatus, action, allowedStatuses);
    }

    /**
     * Checks if a transition from one state to another is allowed.
     *
     * @param from the current state
     * @param to   the target state
     * @return true if the transition is allowed
     */
    public boolean isTransitionAllowed(ExperimentStatus from, ExperimentStatus to) {
        Set<ExperimentStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Gets the allowed target states from a given state.
     *
     * @param from the current state
     * @return set of allowed target states (empty for terminal states)
     */
    public Set<ExperimentStatus> getAllowedTransitions(ExperimentStatus from) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ExperimentStatus.class));
    }

    /**
     * Checks if a status is a terminal state.
     *
     * @param status the status to check
     * @return true if the status is terminal (no further transitions allowed)
     */
    public boolean isTerminalState(ExperimentStatus status) {
        return TERMINAL_STATES.contains(status);
    }
}
