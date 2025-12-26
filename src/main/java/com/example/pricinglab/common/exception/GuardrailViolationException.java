package com.example.pricinglab.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a pricing action violates defined guardrails.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class GuardrailViolationException extends RuntimeException {

    private final String guardrailType;
    private final String violationDetails;

    public GuardrailViolationException(String guardrailType, String violationDetails) {
        super(String.format("Guardrail violation [%s]: %s", guardrailType, violationDetails));
        this.guardrailType = guardrailType;
        this.violationDetails = violationDetails;
    }

    public String getGuardrailType() {
        return guardrailType;
    }

    public String getViolationDetails() {
        return violationDetails;
    }
}
