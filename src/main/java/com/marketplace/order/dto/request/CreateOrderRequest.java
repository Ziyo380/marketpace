package com.marketplace.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @Size(max = 40) String promocode,
        @Valid @NotNull DeliveryDetails delivery
) {}
