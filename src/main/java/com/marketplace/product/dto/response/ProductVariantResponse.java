package com.marketplace.product.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantResponse(UUID id, UUID productId, String sku, String name,
        BigDecimal price, boolean active, int quantity, int reservedQuantity,
        int availableQuantity) {}
