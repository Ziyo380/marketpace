package com.marketplace.product.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderProductImagesRequest(
        @NotEmpty(message = "Image order must not be empty")
        List<@NotNull(message = "Image ID must not be null") UUID> imageIds
) {
}
