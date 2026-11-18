package com.marketplace.category.dto.response;

import java.util.UUID;

public record CategoryResponse(
        UUID id,

        String name,

        String slug,

        String description,

        boolean isActive,

        UUID parentId
) {
}