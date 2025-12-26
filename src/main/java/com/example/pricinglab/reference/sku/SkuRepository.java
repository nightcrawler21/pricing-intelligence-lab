package com.example.pricinglab.reference.sku;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Sku entities.
 */
@Repository
public interface SkuRepository extends JpaRepository<Sku, UUID> {

    Optional<Sku> findBySkuCode(String skuCode);

    List<Sku> findByCategory(String category);

    List<Sku> findByCategoryAndSubcategory(String category, String subcategory);

    List<Sku> findByBrand(String brand);

    List<Sku> findByActiveTrue();

    List<Sku> findByIdIn(List<UUID> ids);
}
