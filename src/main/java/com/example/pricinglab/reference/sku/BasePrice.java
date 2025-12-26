package com.example.pricinglab.reference.sku;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import com.example.pricinglab.reference.store.Store;
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
 * Represents the base (regular) price of a SKU at a specific store.
 *
 * Base prices are the reference point for calculating experiment price changes.
 * They represent the "normal" price before any experimental adjustments.
 *
 * Note: Base price data is typically imported from pricing master systems.
 */
@Entity
@Table(name = "base_prices", indexes = {
    @Index(name = "idx_base_price_sku", columnList = "sku_id"),
    @Index(name = "idx_base_price_store", columnList = "store_id"),
    @Index(name = "idx_base_price_effective", columnList = "effective_date")
})
public class BasePrice extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    /**
     * The base selling price in local currency (THB).
     */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /**
     * Date from which this price is effective.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * Date until which this price is effective (null = indefinite).
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    protected BasePrice() {
        // JPA
    }

    public BasePrice(UUID id, Sku sku, Store store, BigDecimal price, LocalDate effectiveDate) {
        this.id = id;
        this.sku = sku;
        this.store = store;
        this.price = price;
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

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
        BasePrice basePrice = (BasePrice) o;
        return Objects.equals(id, basePrice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
