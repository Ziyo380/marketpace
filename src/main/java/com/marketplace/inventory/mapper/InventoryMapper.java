package com.marketplace.inventory.mapper;

import com.marketplace.inventory.dto.response.InventoryResponse;
import com.marketplace.inventory.entity.Inventory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryMapper {
    public InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProduct().getId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getQuantity()-inventory.getReservedQuantity()
        );
    }

}
