package com.example.pricinglab.reference.sku;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the cost of a SKU (used for margin calculations).
 *
 * Cost data is essential for guardrails that prevent pricing below cost
 * and for calculating projected margin impacts in simulations.
 *
 * Note: Cost data is typically imported from procurement/finance systems.
 */
@Entity
@Table(name = "sku_costs", indexes = {
    @Index(name = "idx_sku_cost_sku", columnList = "sku_id"),
    @Index(name = "idx_sku_cost_effective", columnList = "effective_date")
})
public class SkuCost extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    /**
     * Cost of goods sold (COGS) in local currency (THB).
     */
    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    /**
     * Date from which this cost is effective.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Date until which this cost is effective (null = indefinite).
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    protected SkuCost() {
        // JPA
    }

    public SkuCost(UUID id, Sku sku, BigDecimal cost, LocalDate effectiveDate) {
        this.id = id;
        this.sku = sku;
        this.cost = cost;
        this.effectiveDate = effectiveDate;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Sku getSku() {
        return sku;
    }

    public void setSku(Sku sku) {
        this.sku = sku;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkuCost skuCost = (SkuCost) o;
        return Objects.equals(id, skuCost.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
