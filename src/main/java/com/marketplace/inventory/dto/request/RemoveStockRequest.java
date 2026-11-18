package com.marketplace.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RemoveStockRequest(
        @NotNull
        @Positive
        Integer amount
) {
}
