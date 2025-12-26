package com.example.pricinglab.experiment.domain;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import com.example.pricinglab.common.enums.LeverType;
import com.example.pricinglab.reference.sku.Sku;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Defines a pricing lever (action) to be applied in an experiment.
 *
 * A lever specifies what pricing change will be applied to a SKU
 * in TEST group stores during the experiment period.
 *
 * Examples:
 * - 5% discount on SKU "Coca-Cola 1.5L"
 * - Fixed price of 99 THB for SKU "Lactasoy Soy Milk"
 * - -10 THB off SKU "Lay's Chips 75g"
 */
@Entity
@Table(name = "experiment_levers", indexes = {
    @Index(name = "idx_lever_experiment", columnList = "experiment_id"),
    @Index(name = "idx_lever_sku", columnList = "sku_id")
})
public class ExperimentLever extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "lever_type", nullable = false, length = 30)
    private LeverType leverType;

    /**
     * The value of the lever, interpreted based on leverType:
     * - PERCENTAGE_CHANGE: percentage (e.g., -5.0 means 5% discount)
     * - ABSOLUTE_CHANGE: amount in THB (e.g., -10.00)
     * - TARGET_PRICE: target price in THB (e.g., 99.00)
     */
    @Column(name = "lever_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal leverValue;

    /**
     * Optional description/notes for this lever.
     */
    @Column(name = "description", length = 500)
    private String description;

    protected ExperimentLever() {
        // JPA
    }

    public ExperimentLever(UUID id, Sku sku, LeverType leverType, BigDecimal leverValue) {
        this.id = id;
        this.sku = sku;
        this.leverType = leverType;
        this.leverValue = leverValue;
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
    }

    public Sku getSku() {
        return sku;
    }

    public void setSku(Sku sku) {
        this.sku = sku;
    }

    public LeverType getLeverType() {
        return leverType;
    }

    public void setLeverType(LeverType leverType) {
        this.leverType = leverType;
    }

    public BigDecimal getLeverValue() {
        return leverValue;
    }

    public void setLeverValue(BigDecimal leverValue) {
        this.leverValue = leverValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentLever that = (ExperimentLever) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
