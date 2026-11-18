package com.marketplace.inventory.service;

import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.entity.InventoryMovement;
import com.marketplace.inventory.entity.InventoryMovementType;
import com.marketplace.inventory.repository.InventoryMovementRepository;
import com.marketplace.product.entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryMovementService {
    private final InventoryMovementRepository repository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Inventory inventory, InventoryMovementType type, int quantityDelta, int reservedDelta) {
        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setType(type);
        movement.setQuantityDelta(quantityDelta);
        movement.setReservedDelta(reservedDelta);
        movement.setQuantityAfter(inventory.getQuantity());
        movement.setReservedAfter(inventory.getReservedQuantity());
        repository.save(movement);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(ProductVariant variant, InventoryMovementType type,
                       int quantityDelta, int reservedDelta) {
        InventoryMovement movement = new InventoryMovement();
        movement.setVariant(variant);
        movement.setType(type);
        movement.setQuantityDelta(quantityDelta);
        movement.setReservedDelta(reservedDelta);
        movement.setQuantityAfter(variant.getQuantity());
        movement.setReservedAfter(variant.getReservedQuantity());
        repository.save(movement);
    }
}
