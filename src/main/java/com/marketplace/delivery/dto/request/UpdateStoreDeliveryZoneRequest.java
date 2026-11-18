package com.marketplace.delivery.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStoreDeliveryZoneRequest(
        @NotNull
        @DecimalMin("0.0")
        BigDecimal deliveryFee,

        @NotNull
        @Min(1)
        Integer minDeliveryDays,

        @NotNull
        @Min(1)
        Integer maxDeliveryDays
) {
}
