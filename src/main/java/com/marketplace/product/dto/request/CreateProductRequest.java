package com.marketplace.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull
        UUID categoryId,

        @NotBlank
        @Size(max = 200)
        String name,

        @NotBlank
        @Size(max = 220)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must contain lowercase letters, numbers and hyphens"
        )
        String slug,

        String description,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal price
) {
}
