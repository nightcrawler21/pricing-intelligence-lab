package com.example.pricinglab.experiment.domain;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import com.example.pricinglab.common.enums.ExperimentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a pricing experiment.
 *
 * An experiment defines:
 * - Which stores and SKUs are affected (scope)
 * - What pricing changes to apply (levers)
 * - What limits must be respected (guardrails)
 * - The time period for the experiment
 *
 * Experiments must be approved before simulations can be run.
 * This is a SIMULATION ONLY tool - no actual pricing changes are made.
 */
@Entity
@Table(name = "experiments", indexes = {
    @Index(name = "idx_experiment_status", columnList = "status"),
    @Index(name = "idx_experiment_start_date", columnList = "start_date"),
    @Index(name = "idx_experiment_created_by", columnList = "created_by")
})
public class Experiment extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Human-readable name for the experiment.
     * Example: "Q1 2024 Beverage Price Sensitivity Test"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Detailed description of the experiment's purpose and hypothesis.
     */
    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExperimentStatus status = ExperimentStatus.DRAFT;

    /**
     * Planned start date for the experiment.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Planned end date for the experiment.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Business justification for the experiment.
     */
    @Column(name = "business_justification", length = 2000)
    private String businessJustification;

    /**
     * The hypothesis being tested.
     * Example: "5% discount on soft drinks will increase units sold by 10%"
     */
    @Column(name = "hypothesis", length = 1000)
    private String hypothesis;

    /**
     * Username of the admin who approved/rejected the experiment.
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * Reason for rejection (if status is REJECTED).
     */
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExperimentScope> scopes = new ArrayList<>();

    @OneToMany(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExperimentLever> levers = new ArrayList<>();

    @OneToOne(mappedBy = "experiment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ExperimentGuardrails guardrails;

    protected Experiment() {
        // JPA
    }

    public Experiment(UUID id, String name, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Helper methods

    public void addScope(ExperimentScope scope) {
        scopes.add(scope);
        scope.setExperiment(this);
    }

    public void removeScope(ExperimentScope scope) {
        scopes.remove(scope);
        scope.setExperiment(null);
    }

    public void addLever(ExperimentLever lever) {
        levers.add(lever);
        lever.setExperiment(this);
    }

    public void removeLever(ExperimentLever lever) {
        levers.remove(lever);
        lever.setExperiment(null);
    }

    public void setGuardrails(ExperimentGuardrails guardrails) {
        this.guardrails = guardrails;
        if (guardrails != null) {
            guardrails.setExperiment(this);
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ExperimentStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public String getHypothesis() {
        return hypothesis;
    }

    public void setHypothesis(String hypothesis) {
        this.hypothesis = hypothesis;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<ExperimentScope> getScopes() {
        return scopes;
    }

    public List<ExperimentLever> getLevers() {
        return levers;
    }

    public ExperimentGuardrails getGuardrails() {
        return guardrails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Experiment that = (Experiment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Experiment{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", status=" + status +
            '}';
    }
}
