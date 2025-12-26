package com.example.pricinglab.reference.sku;

import com.example.pricinglab.common.audit.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a Stock Keeping Unit (SKU) - a specific product.
 *
 * SKUs are the products that can have price experiments applied to them.
 *
 * Note: SKU data is typically imported from product master data systems.
 */
@Entity
@Table(name = "skus", indexes = {
    @Index(name = "idx_sku_code", columnList = "sku_code"),
    @Index(name = "idx_sku_category", columnList = "category"),
    @Index(name = "idx_sku_active", columnList = "is_active")
})
public class Sku extends BaseAuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * External SKU code from product master (e.g., "8850999123456").
     * Typically a barcode or internal product ID.
     */
    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "sku_name", nullable = false, length = 300)
    private String skuName;

    /**
     * Product category for grouping (e.g., "Beverages", "Dairy", "Snacks").
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Subcategory for finer grouping (e.g., "Carbonated Drinks", "Fresh Milk").
     */
    @Column(name = "subcategory", length = 100)
    private String subcategory;

    /**
     * Brand name (e.g., "Coca-Cola", "Lactasoy").
     */
    @Column(name = "brand", length = 100)
    private String brand;

    /**
     * Unit of measure (e.g., "EA", "KG", "L").
     */
    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Sku() {
        // JPA
    }

    public Sku(UUID id, String skuCode, String skuName) {
        this.id = id;
        this.skuCode = skuCode;
        this.skuName = skuName;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
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
        Sku sku = (Sku) o;
        return Objects.equals(id, sku.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Sku{" +
            "id=" + id +
            ", skuCode='" + skuCode + '\'' +
            ", skuName='" + skuName + '\'' +
            '}';
    }
}
