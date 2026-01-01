package com.example.pricinglab.common.exception;

import com.example.pricinglab.common.enums.ExperimentStatus;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception thrown when an action is attempted on an experiment that is not in the required state.
 *
 * <p>This exception provides clear messaging about:</p>
 * <ul>
 *   <li>The current state of the experiment</li>
 *   <li>The action that was attempted</li>
 *   <li>Which state(s) would allow the action</li>
 * </ul>
 *
 * <p>Returns HTTP 400 Bad Request to indicate client error (attempting invalid operation).</p>
 */
public class InvalidExperimentStateException extends RuntimeException {

    private final ExperimentStatus currentStatus;
    private final String attemptedAction;
    private final Set<ExperimentStatus> allowedStatuses;

    /**
     * Creates an exception for an invalid experiment state.
     *
     * @param currentStatus   the current status of the experiment
     * @param attemptedAction the action that was attempted (e.g., "submit", "approve", "start simulation")
     * @param allowedStatuses the status(es) that would allow this action
     */
    public InvalidExperimentStateException(
            ExperimentStatus currentStatus,
            String attemptedAction,
            ExperimentStatus... allowedStatuses) {
        super(buildMessage(currentStatus, attemptedAction, allowedStatuses));
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
        this.allowedStatuses = Arrays.stream(allowedStatuses).collect(Collectors.toSet());
    }

    private static String buildMessage(
            ExperimentStatus currentStatus,
            String attemptedAction,
            ExperimentStatus... allowedStatuses) {
        String allowedList = Arrays.stream(allowedStatuses)
                .map(ExperimentStatus::name)
                .collect(Collectors.joining(", "));

        String stateWord = allowedStatuses.length == 1 ? "state" : "states";

        return String.format(
                "Cannot %s experiment in state %s. Only experiments in %s %s can be %s.",
                attemptedAction,
                currentStatus,
                stateWord,
                allowedList,
                getPassiveAction(attemptedAction)
        );
    }

    /**
     * Converts action to passive form for message construction.
     */
    private static String getPassiveAction(String action) {
        return switch (action.toLowerCase()) {
            case "submit" -> "submitted";
            case "approve" -> "approved";
            case "reject" -> "rejected";
            case "start simulation" -> "started";
            case "complete simulation" -> "completed";
            case "fail simulation" -> "marked as failed";
            default -> action + "ed";
        };
    }

    public ExperimentStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }

    public Set<ExperimentStatus> getAllowedStatuses() {
        return allowedStatuses;
    }
}
