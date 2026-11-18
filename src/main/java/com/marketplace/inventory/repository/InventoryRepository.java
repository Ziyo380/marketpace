package com.marketplace.inventory.repository;

import com.marketplace.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProduct_Id(UUID product_id);

    boolean existsByProduct_Id(UUID product_id);
}
