package com.marketplace.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        @Size(min = 8)
        String password
) {}
