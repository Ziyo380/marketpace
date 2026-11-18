package com.marketplace.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 120)
        String slug,

        @Size(max = 1000)
        String description,

        @NotNull
        Boolean active,

        UUID parentId

) {
}
