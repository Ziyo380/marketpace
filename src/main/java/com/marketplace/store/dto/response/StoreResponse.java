package com.marketplace.store.dto.response;

import java.util.UUID;

public record StoreResponse(
        UUID id,
        UUID sellerId,
        String name,
        String slug,
        String description,
        boolean active,
        boolean approved,
        String rejectionReason
) {
}
