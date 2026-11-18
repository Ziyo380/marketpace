package com.marketplace.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 180)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must contain lowercase letters, numbers and hyphens"
        )
        String slug,

        @Size(max = 1000)
        String description

) {}
