package com.example.pricinglab.common.exception;

import com.example.pricinglab.common.enums.ExperimentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid experiment state transition is attempted.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends RuntimeException {

    private final ExperimentStatus currentStatus;
    private final ExperimentStatus targetStatus;

    public InvalidStateTransitionException(ExperimentStatus currentStatus, ExperimentStatus targetStatus) {
        super(String.format("Cannot transition from %s to %s", currentStatus, targetStatus));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public ExperimentStatus getCurrentStatus() {
        return currentStatus;
    }

    public ExperimentStatus getTargetStatus() {
        return targetStatus;
    }
}
