package com.marketplace.inventory.dto.response;

import com.marketplace.inventory.entity.InventoryMovementType;
import java.time.Instant;
import java.util.UUID;

public record InventoryMovementResponse(UUID id, InventoryMovementType type,
        int quantityDelta, int reservedDelta, int quantityAfter,
        int reservedAfter, Instant createdAt) {}
