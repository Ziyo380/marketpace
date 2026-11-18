package com.marketplace.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull
        UUID productId,

        @NotNull
        @Positive
        Integer quantity,
        UUID variantId
) {
}
