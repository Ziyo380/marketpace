
package com.marketplace.product.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        UUID productId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String url,
        boolean primary,
        int sortOrder,
        Instant createdAt
) {
}
