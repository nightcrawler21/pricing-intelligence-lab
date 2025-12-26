package com.example.pricinglab.reference.store;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a physical retail store location.
 *
 * Stores are the fundamental unit for pricing experiments in this system.
 * Each store can be assigned to either a control or test group.
 *
 * Note: Store data is typically imported from master data systems.
 */
@Entity
@Table(name = "stores", indexes = {
    @Index(name = "idx_store_code", columnList = "store_code"),
    @Index(name = "idx_store_region", columnList = "region"),
    @Index(name = "idx_store_active", columnList = "is_active")
})
public class Store extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * External store code from master data (e.g., "BKK001").
     */
    @Column(name = "store_code", nullable = false, unique = true, length = 50)
    private String storeCode;

    @Column(name = "store_name", nullable = false, length = 200)
    private String storeName;

    /**
     * Region for grouping stores (e.g., "Bangkok", "Central", "Northeast").
     */
    @Column(name = "region", length = 100)
    private String region;

    /**
     * Store format (e.g., "Hypermarket", "Supermarket", "Express").
     */
    @Column(name = "format", length = 50)
    private String format;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Store() {
        // JPA
    }

    public Store(UUID id, String storeCode, String storeName) {
        this.id = id;
        this.storeCode = storeCode;
        this.storeName = storeName;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Store store = (Store) o;
        return Objects.equals(id, store.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Store{" +
            "id=" + id +
            ", storeCode='" + storeCode + '\'' +
            ", storeName='" + storeName + '\'' +
            '}';
    }
}
