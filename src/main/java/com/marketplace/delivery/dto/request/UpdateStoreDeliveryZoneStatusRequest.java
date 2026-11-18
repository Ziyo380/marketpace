package com.marketplace.delivery.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateStoreDeliveryZoneStatusRequest(
        @NotNull
        Boolean active
) {
}
