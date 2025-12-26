package com.example.pricinglab.experiment.domain;

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

import java.util.Objects;
import java.util.UUID;

/**
 * Defines the scope of an experiment: which store-SKU combinations are included.
 *
 * Each scope entry represents one store-SKU pair that is part of the experiment.
 * The isTestGroup flag determines whether this combination is in the TEST group
 * (receiving the experimental pricing) or CONTROL group (baseline pricing).
 */
@Entity
@Table(name = "experiment_scopes", indexes = {
    @Index(name = "idx_scope_experiment", columnList = "experiment_id"),
    @Index(name = "idx_scope_store", columnList = "store_id"),
    @Index(name = "idx_scope_sku", columnList = "sku_id")
})
public class ExperimentScope extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_id", nullable = false)
    private Experiment experiment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private Sku sku;

    /**
     * True if this store-SKU is in the TEST group, false if in CONTROL group.
     */
    @Column(name = "is_test_group", nullable = false)
    private boolean testGroup;

    protected ExperimentScope() {
        // JPA
    }

    public ExperimentScope(UUID id, Store store, Sku sku, boolean testGroup) {
        this.id = id;
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

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentScope that = (ExperimentScope) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
