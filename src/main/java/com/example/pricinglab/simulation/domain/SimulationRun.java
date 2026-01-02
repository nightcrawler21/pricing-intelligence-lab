package com.example.pricinglab.simulation.domain;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import com.example.pricinglab.common.enums.SimulationStatus;
import com.example.pricinglab.experiment.domain.Experiment;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a simulation run for an experiment.
 *
 * A simulation run calculates the projected outcomes of an experiment
 * by applying the defined levers to historical data and estimating
 * the impact on sales, revenue, and margin.
 *
 * Multiple simulation runs can be performed for the same experiment
 * (e.g., with different assumptions or parameters).
 */
@Entity
@Table(name = "simulation_runs", indexes = {
    @Index(name = "idx_sim_run_experiment", columnList = "experiment_id"),
    @Index(name = "idx_sim_run_status", columnList = "status"),
    @Index(name = "idx_sim_run_started", columnList = "started_at")
})
public class SimulationRun extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationStatus status = SimulationStatus.PENDING;

    /**
     * Timestamp when simulation execution started.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Timestamp when simulation execution completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Error message if simulation failed.
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Total number of days simulated (startDate to endDate inclusive).
     */
    @Column(name = "total_days_simulated")
    private Integer totalDaysSimulated;

    // Summary metrics (populated after completion)

    /**
     * Projected total revenue for TEST group.
     */
    @Column(name = "projected_revenue_test", precision = 15, scale = 2)
    private BigDecimal projectedRevenueTest;

    /**
     * Projected total revenue for CONTROL group.
     */
    @Column(name = "projected_revenue_control", precision = 15, scale = 2)
    private BigDecimal projectedRevenueControl;

    /**
     * Projected revenue lift (TEST - CONTROL) / CONTROL as percentage.
     */
    @Column(name = "projected_revenue_lift_pct", precision = 8, scale = 4)
    private BigDecimal projectedRevenueLiftPct;

    /**
     * Projected total units sold for TEST group.
     */
    @Column(name = "projected_units_test", precision = 15, scale = 2)
    private BigDecimal projectedUnitsTest;

    /**
     * Projected total units sold for CONTROL group.
     */
    @Column(name = "projected_units_control", precision = 15, scale = 2)
    private BigDecimal projectedUnitsControl;

    /**
     * Projected gross margin for TEST group.
     */
    @Column(name = "projected_margin_test", precision = 15, scale = 2)
    private BigDecimal projectedMarginTest;

    /**
     * Projected gross margin for CONTROL group.
     */
    @Column(name = "projected_margin_control", precision = 15, scale = 2)
    private BigDecimal projectedMarginControl;

    @OneToMany(mappedBy = "simulationRun", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SimulationResultDaily> dailyResults = new ArrayList<>();

    protected SimulationRun() {
        // JPA
    }

    public SimulationRun(UUID id, Experiment experiment) {
        this.id = id;
        this.experiment = experiment;
    }

    // Helper methods

    public void addDailyResult(SimulationResultDaily result) {
        dailyResults.add(result);
        result.setSimulationRun(this);
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

    public SimulationStatus getStatus() {
        return status;
    }

    public void setStatus(SimulationStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getTotalDaysSimulated() {
        return totalDaysSimulated;
    }

    public void setTotalDaysSimulated(Integer totalDaysSimulated) {
        this.totalDaysSimulated = totalDaysSimulated;
    }

    public BigDecimal getProjectedRevenueTest() {
        return projectedRevenueTest;
    }

    public void setProjectedRevenueTest(BigDecimal projectedRevenueTest) {
        this.projectedRevenueTest = projectedRevenueTest;
    }

    public BigDecimal getProjectedRevenueControl() {
        return projectedRevenueControl;
    }

    public void setProjectedRevenueControl(BigDecimal projectedRevenueControl) {
        this.projectedRevenueControl = projectedRevenueControl;
    }

    public BigDecimal getProjectedRevenueLiftPct() {
        return projectedRevenueLiftPct;
    }

    public void setProjectedRevenueLiftPct(BigDecimal projectedRevenueLiftPct) {
        this.projectedRevenueLiftPct = projectedRevenueLiftPct;
    }

    public BigDecimal getProjectedUnitsTest() {
        return projectedUnitsTest;
    }

    public void setProjectedUnitsTest(BigDecimal projectedUnitsTest) {
        this.projectedUnitsTest = projectedUnitsTest;
    }

    public BigDecimal getProjectedUnitsControl() {
        return projectedUnitsControl;
    }

    public void setProjectedUnitsControl(BigDecimal projectedUnitsControl) {
        this.projectedUnitsControl = projectedUnitsControl;
    }

    public BigDecimal getProjectedMarginTest() {
        return projectedMarginTest;
    }

    public void setProjectedMarginTest(BigDecimal projectedMarginTest) {
        this.projectedMarginTest = projectedMarginTest;
    }

    public BigDecimal getProjectedMarginControl() {
        return projectedMarginControl;
    }

    public void setProjectedMarginControl(BigDecimal projectedMarginControl) {
        this.projectedMarginControl = projectedMarginControl;
    }

    public List<SimulationResultDaily> getDailyResults() {
        return dailyResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimulationRun that = (SimulationRun) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SimulationRun{" +
            "id=" + id +
            ", experimentId=" + (experiment != null ? experiment.getId() : null) +
            ", status=" + status +
            '}';
    }
}
