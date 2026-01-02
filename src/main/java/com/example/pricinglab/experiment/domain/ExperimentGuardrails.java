package com.example.pricinglab.experiment.domain;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Defines the guardrails (constraints) for an experiment.
 *
 * Guardrails prevent pricing experiments from causing harm by enforcing:
 * - Maximum discount percentages
 * - Minimum margin requirements
 * - Revenue impact limits
 *
 * All experiments must have guardrails configured before approval.
 *
 * <p><strong>Design Note (v0):</strong> This entity uses {@code @MapsId} to share the
 * experiment's primary key, enforcing a strict 1:1 relationship. This simplifies the
 * data model but means guardrails cannot be versioned independently or reused as
 * templates across experiments. If guardrail versioning or templates become requirements,
 * this entity will need its own UUID and a separate FK to experiment.</p>
 */
@Entity
@Table(name = "experiment_guardrails")
public class ExperimentGuardrails extends BaseAuditableEntity {

    /**
     * Shared primary key with Experiment (via @MapsId).
     * This enforces exactly one guardrails record per experiment.
     */
    @Id
    @Column(name = "experiment_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "experiment_id")
    private Experiment experiment;

    /**
     * Maximum allowed discount percentage (e.g., 20.0 means max 20% off).
     * Prevents excessive discounting that could erode margins.
     */
    @Column(name = "max_discount_pct", precision = 5, scale = 2)
    private BigDecimal maxDiscountPercentage;

    /**
     * Maximum allowed markup percentage (e.g., 15.0 means max 15% above base).
     * Prevents price increases that could hurt sales or brand perception.
     */
    @Column(name = "max_markup_pct", precision = 5, scale = 2)
    private BigDecimal maxMarkupPercentage;

    /**
     * Minimum required gross margin percentage (e.g., 5.0 means min 5% margin).
     * Prevents pricing below sustainable margin thresholds.
     */
    @Column(name = "min_margin_pct", precision = 5, scale = 2)
    private BigDecimal minMarginPercentage;

    /**
     * Maximum allowed revenue impact as percentage of baseline (e.g., -10.0).
     * Limits the potential negative business impact of the experiment.
     */
    @Column(name = "max_revenue_impact_pct", precision = 5, scale = 2)
    private BigDecimal maxRevenueImpactPercentage;

    /**
     * If true, prices cannot go below cost (ensures no loss-leader situations).
     */
    @Column(name = "prevent_below_cost", nullable = false)
    private boolean preventBelowCost = true;

    /**
     * If true, prices must end in .00, .25, .50, or .75 (price point enforcement).
     */
    @Column(name = "enforce_price_points", nullable = false)
    private boolean enforcePricePoints = false;

    // --- v0 Guardrail Fields ---

    /**
     * v0: Minimum allowed resulting price in THB.
     * The lever-implied price must be >= this value.
     */
    @Column(name = "price_floor", precision = 12, scale = 2)
    private BigDecimal priceFloor;

    /**
     * v0: Maximum allowed resulting price in THB.
     * The lever-implied price must be <= this value.
     */
    @Column(name = "price_ceiling", precision = 12, scale = 2)
    private BigDecimal priceCeiling;

    /**
     * v0: Maximum allowed absolute percentage change from base price.
     * Example: 20.0 means price can change at most 20% from base.
     */
    @Column(name = "max_change_percent", precision = 5, scale = 2)
    private BigDecimal maxChangePercent;

    protected ExperimentGuardrails() {
        // JPA
    }

    public ExperimentGuardrails(UUID experimentId) {
        this.id = experimentId;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
        this.id = experiment != null ? experiment.getId() : null;
    }

    public BigDecimal getMaxDiscountPercentage() {
        return maxDiscountPercentage;
    }

    public void setMaxDiscountPercentage(BigDecimal maxDiscountPercentage) {
        this.maxDiscountPercentage = maxDiscountPercentage;
    }

    public BigDecimal getMaxMarkupPercentage() {
        return maxMarkupPercentage;
    }

    public void setMaxMarkupPercentage(BigDecimal maxMarkupPercentage) {
        this.maxMarkupPercentage = maxMarkupPercentage;
    }

    public BigDecimal getMinMarginPercentage() {
        return minMarginPercentage;
    }

    public void setMinMarginPercentage(BigDecimal minMarginPercentage) {
        this.minMarginPercentage = minMarginPercentage;
    }

    public BigDecimal getMaxRevenueImpactPercentage() {
        return maxRevenueImpactPercentage;
    }

    public void setMaxRevenueImpactPercentage(BigDecimal maxRevenueImpactPercentage) {
        this.maxRevenueImpactPercentage = maxRevenueImpactPercentage;
    }

    public boolean isPreventBelowCost() {
        return preventBelowCost;
    }

    public void setPreventBelowCost(boolean preventBelowCost) {
        this.preventBelowCost = preventBelowCost;
    }

    public boolean isEnforcePricePoints() {
        return enforcePricePoints;
    }

    public void setEnforcePricePoints(boolean enforcePricePoints) {
        this.enforcePricePoints = enforcePricePoints;
    }

    // --- v0 Getters and Setters ---

    public BigDecimal getPriceFloor() {
        return priceFloor;
    }

    public void setPriceFloor(BigDecimal priceFloor) {
        this.priceFloor = priceFloor;
    }

    public BigDecimal getPriceCeiling() {
        return priceCeiling;
    }

    public void setPriceCeiling(BigDecimal priceCeiling) {
        this.priceCeiling = priceCeiling;
    }

    public BigDecimal getMaxChangePercent() {
        return maxChangePercent;
    }

    public void setMaxChangePercent(BigDecimal maxChangePercent) {
        this.maxChangePercent = maxChangePercent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentGuardrails that = (ExperimentGuardrails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
