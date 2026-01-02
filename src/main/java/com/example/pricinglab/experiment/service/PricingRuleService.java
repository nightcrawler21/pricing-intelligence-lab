package com.example.pricinglab.experiment.service;

import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.common.exception.GuardrailViolationException;
import com.example.pricinglab.experiment.domain.Experiment;
import com.example.pricinglab.experiment.domain.ExperimentGuardrails;
import com.example.pricinglab.experiment.domain.ExperimentLever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Service for pricing rule calculations and guardrail validation.
 *
 * Handles:
 * - Calculating test prices based on levers
 * - Validating pricing changes against guardrails
 * - Enforcing margin and discount limits
 */
@Service
public class PricingRuleService {

    private static final Logger log = LoggerFactory.getLogger(PricingRuleService.class);

    /**
     * Calculates the test price for a SKU based on a lever.
     *
     * @param basePrice the current base price
     * @param lever the pricing lever to apply
     * @return the calculated test price
     */
    public BigDecimal calculateTestPrice(BigDecimal basePrice, ExperimentLever lever) {
        // TODO: Implement price calculation logic

        return switch (lever.getLeverType()) {
            case PRICE_DISCOUNT -> {
                // Apply discount percentage (e.g., 10.0 = 10% off)
                BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    lever.getLeverValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                );
                yield basePrice.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
            }
            case PERCENTAGE_CHANGE -> {
                // Apply percentage change (negative = discount, positive = markup)
                BigDecimal multiplier = BigDecimal.ONE.add(
                    lever.getLeverValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                );
                yield basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            }
            case ABSOLUTE_CHANGE -> {
                // Apply absolute change
                yield basePrice.add(lever.getLeverValue()).setScale(2, RoundingMode.HALF_UP);
            }
            case TARGET_PRICE -> {
                // Use the target price directly
                yield lever.getLeverValue().setScale(2, RoundingMode.HALF_UP);
            }
            case COMPETITOR_MATCH -> {
                // TODO: Not implemented in v0 - would need competitor price data
                log.warn("COMPETITOR_MATCH lever type not yet implemented");
                yield basePrice;
            }
        };
    }

    /**
     * Validates that a lever complies with the experiment's guardrails.
     *
     * @param lever the lever to validate
     * @param basePrice the current base price
     * @param cost the product cost (for margin calculations)
     * @param guardrails the guardrails to validate against
     * @throws GuardrailViolationException if a guardrail is violated
     */
    public void validateLeverAgainstGuardrails(
        ExperimentLever lever,
        BigDecimal basePrice,
        BigDecimal cost,
        ExperimentGuardrails guardrails
    ) {
        // TODO: Implement full guardrail validation

        BigDecimal testPrice = calculateTestPrice(basePrice, lever);

        // Check prevent below cost
        if (guardrails.isPreventBelowCost() && cost != null && testPrice.compareTo(cost) < 0) {
            throw new GuardrailViolationException(
                "PREVENT_BELOW_COST",
                String.format("Test price %.2f is below cost %.2f", testPrice, cost)
            );
        }

        // Check max discount
        if (guardrails.getMaxDiscountPercentage() != null) {
            BigDecimal discountPct = basePrice.subtract(testPrice)
                .divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            if (discountPct.compareTo(guardrails.getMaxDiscountPercentage()) > 0) {
                throw new GuardrailViolationException(
                    "MAX_DISCOUNT",
                    String.format("Discount %.2f%% exceeds maximum %.2f%%",
                        discountPct, guardrails.getMaxDiscountPercentage())
                );
            }
        }

        // Check max markup
        if (guardrails.getMaxMarkupPercentage() != null) {
            BigDecimal markupPct = testPrice.subtract(basePrice)
                .divide(basePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            if (markupPct.compareTo(guardrails.getMaxMarkupPercentage()) > 0) {
                throw new GuardrailViolationException(
                    "MAX_MARKUP",
                    String.format("Markup %.2f%% exceeds maximum %.2f%%",
                        markupPct, guardrails.getMaxMarkupPercentage())
                );
            }
        }

        // Check minimum margin
        if (guardrails.getMinMarginPercentage() != null && cost != null) {
            BigDecimal margin = testPrice.subtract(cost)
                .divide(testPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            if (margin.compareTo(guardrails.getMinMarginPercentage()) < 0) {
                throw new GuardrailViolationException(
                    "MIN_MARGIN",
                    String.format("Margin %.2f%% is below minimum %.2f%%",
                        margin, guardrails.getMinMarginPercentage())
                );
            }
        }

        // TODO: Check price points enforcement
        // TODO: Check revenue impact (requires historical sales data)
    }

    /**
     * Validates all levers in an experiment against guardrails.
     *
     * @param experiment the experiment to validate
     * @throws GuardrailViolationException if any guardrail is violated
     */
    public void validateExperimentGuardrails(Experiment experiment) {
        // TODO: Implement full experiment validation
        // This would iterate through all levers and validate each one

        ExperimentGuardrails guardrails = experiment.getGuardrails();
        if (guardrails == null) {
            throw new GuardrailViolationException(
                "MISSING_GUARDRAILS",
                "Experiment must have guardrails configured before approval"
            );
        }

        List<ExperimentLever> levers = experiment.getLevers();
        if (levers.isEmpty()) {
            throw new GuardrailViolationException(
                "NO_LEVERS",
                "Experiment must have at least one pricing lever defined"
            );
        }

        // TODO: For each lever, fetch base price and cost, then validate
        log.info("Guardrail validation completed for experiment {}", experiment.getId());
    }
}
