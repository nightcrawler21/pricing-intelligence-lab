package com.example.pricinglab.reference.sales;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import com.example.pricinglab.reference.sku.Sku;
import com.example.pricinglab.reference.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents baseline (historical) daily sales for a SKU at a specific store.
 *
 * <p>Baseline sales data is used by the simulation engine to project how
 * units sold would change under different pricing scenarios. Currently,
 * the v0 simulation uses a fixed baseline of 100 units/day, but future
 * versions will load from this historical data.</p>
 *
 * <p>Each record represents the actual sales for a single store-SKU-date
 * combination. This data is typically imported from sales/POS systems.</p>
 */
@Entity
@Table(name = "baseline_daily_sales",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_baseline_store_sku_date",
            columnNames = {"store_id", "sku_id", "sales_date"})
    },
    indexes = {
        @Index(name = "idx_baseline_store", columnList = "store_id"),
        @Index(name = "idx_baseline_sku", columnList = "sku_id"),
        @Index(name = "idx_baseline_date", columnList = "sales_date"),
        @Index(name = "idx_baseline_store_sku", columnList = "store_id, sku_id")
    })
public class BaselineDailySales extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    /**
     * The date of the sales record.
     */
    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    /**
     * Number of units sold on this date.
     */
    @Column(name = "units_sold", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitsSold;

    /**
     * Revenue generated (optional, can be derived from price × units).
     */
    @Column(name = "revenue", precision = 12, scale = 2)
    private BigDecimal revenue;

    protected BaselineDailySales() {
        // JPA
    }

    public BaselineDailySales(UUID id, Store store, Sku sku, LocalDate salesDate, BigDecimal unitsSold) {
        this.id = id;
        this.store = store;
        this.sku = sku;
        this.salesDate = salesDate;
        this.unitsSold = unitsSold;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Sku getSku() {
        return sku;
    }

    public void setSku(Sku sku) {
        this.sku = sku;
    }

    public LocalDate getSalesDate() {
        return salesDate;
    }

    public void setSalesDate(LocalDate salesDate) {
        this.salesDate = salesDate;
    }

    public BigDecimal getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(BigDecimal unitsSold) {
        this.unitsSold = unitsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaselineDailySales that = (BaselineDailySales) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BaselineDailySales{" +
            "id=" + id +
            ", salesDate=" + salesDate +
            ", unitsSold=" + unitsSold +
            '}';
    }
}
