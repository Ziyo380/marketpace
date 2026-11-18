package com.marketplace.inventory.dto.response;

import java.util.UUID;

public record InventoryResponse(
        UUID productId,

        int quantity,

        int reservedQuantity,

        int availableQuantity
) {
}
