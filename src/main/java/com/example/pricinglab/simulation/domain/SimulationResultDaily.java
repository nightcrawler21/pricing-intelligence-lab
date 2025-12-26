package com.example.pricinglab.simulation.domain;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Daily granularity results for a simulation run.
 *
 * Stores projected metrics for each store-SKU-date combination,
 * allowing for detailed analysis of expected experiment outcomes.
 */
@Entity
@Table(name = "simulation_results_daily", indexes = {
    @Index(name = "idx_sim_result_run", columnList = "simulation_run_id"),
    @Index(name = "idx_sim_result_date", columnList = "simulation_date"),
    @Index(name = "idx_sim_result_store", columnList = "store_id"),
    @Index(name = "idx_sim_result_sku", columnList = "sku_id")
})
public class SimulationResultDaily {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_run_id", nullable = false)
    private SimulationRun simulationRun;

    @Column(name = "simulation_date", nullable = false)
    private LocalDate simulationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    /**
     * True if this is a TEST group result, false for CONTROL.
     */
    @Column(name = "is_test_group", nullable = false)
    private boolean testGroup;

    /**
     * The base price used in simulation.
     */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    /**
     * The simulated price (equals basePrice for CONTROL, modified for TEST).
     */
    @Column(name = "simulated_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal simulatedPrice;

    /**
     * Projected units sold.
     */
    @Column(name = "projected_units", nullable = false, precision = 12, scale = 2)
    private BigDecimal projectedUnits;

    /**
     * Projected revenue (simulatedPrice * projectedUnits).
     */
    @Column(name = "projected_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal projectedRevenue;

    /**
     * Projected gross margin.
     */
    @Column(name = "projected_margin", nullable = false, precision = 15, scale = 2)
    private BigDecimal projectedMargin;

    /**
     * Historical baseline units (from prior period or comparable).
     */
    @Column(name = "baseline_units", precision = 12, scale = 2)
    private BigDecimal baselineUnits;

    /**
     * Historical baseline revenue.
     */
    @Column(name = "baseline_revenue", precision = 15, scale = 2)
    private BigDecimal baselineRevenue;

    protected SimulationResultDaily() {
        // JPA
    }

    public SimulationResultDaily(UUID id, LocalDate simulationDate, Store store, Sku sku, boolean testGroup) {
        this.id = id;
        this.simulationDate = simulationDate;
        this.store = store;
        this.sku = sku;
        this.testGroup = testGroup;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SimulationRun getSimulationRun() {
        return simulationRun;
    }

    public void setSimulationRun(SimulationRun simulationRun) {
        this.simulationRun = simulationRun;
    }

    public LocalDate getSimulationDate() {
        return simulationDate;
    }

    public void setSimulationDate(LocalDate simulationDate) {
        this.simulationDate = simulationDate;
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

    public boolean isTestGroup() {
        return testGroup;
    }

    public void setTestGroup(boolean testGroup) {
        this.testGroup = testGroup;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getSimulatedPrice() {
        return simulatedPrice;
    }

    public void setSimulatedPrice(BigDecimal simulatedPrice) {
        this.simulatedPrice = simulatedPrice;
    }

    public BigDecimal getProjectedUnits() {
        return projectedUnits;
    }

    public void setProjectedUnits(BigDecimal projectedUnits) {
        this.projectedUnits = projectedUnits;
    }

    public BigDecimal getProjectedRevenue() {
        return projectedRevenue;
    }

    public void setProjectedRevenue(BigDecimal projectedRevenue) {
        this.projectedRevenue = projectedRevenue;
    }

    public BigDecimal getProjectedMargin() {
        return projectedMargin;
    }

    public void setProjectedMargin(BigDecimal projectedMargin) {
        this.projectedMargin = projectedMargin;
    }

    public BigDecimal getBaselineUnits() {
        return baselineUnits;
    }

    public void setBaselineUnits(BigDecimal baselineUnits) {
        this.baselineUnits = baselineUnits;
    }

    public BigDecimal getBaselineRevenue() {
        return baselineRevenue;
    }

    public void setBaselineRevenue(BigDecimal baselineRevenue) {
        this.baselineRevenue = baselineRevenue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimulationResultDaily that = (SimulationResultDaily) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
