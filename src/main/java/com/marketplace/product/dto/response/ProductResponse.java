package com.marketplace.product.dto.response;

import com.marketplace.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,

        UUID storeId,

        String storeSlug,

        UUID categoryId,

        String name,

        String slug,

        String description,

        ProductStatus status,

        Instant createdAt,

        Instant updatedAt,

        BigDecimal price,

        boolean moderationBlocked
) {
}
