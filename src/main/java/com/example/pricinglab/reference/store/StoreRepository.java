package com.example.pricinglab.reference.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Store entities.
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

    Optional<Store> findByStoreCode(String storeCode);

    List<Store> findByRegion(String region);

    List<Store> findByFormat(String format);

    List<Store> findByActiveTrue();

    List<Store> findByIdIn(List<UUID> ids);
}
