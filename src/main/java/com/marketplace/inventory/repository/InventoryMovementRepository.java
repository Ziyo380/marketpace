package com.marketplace.inventory.repository;

import com.marketplace.inventory.entity.InventoryMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findAllByInventory_IdOrderByCreatedAtAsc(UUID inventoryId);
    List<InventoryMovement> findAllByVariant_IdOrderByCreatedAtAsc(UUID variantId);
}
