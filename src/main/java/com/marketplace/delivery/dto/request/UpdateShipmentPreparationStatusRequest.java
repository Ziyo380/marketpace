package com.marketplace.delivery.dto.request;

import com.marketplace.delivery.entity.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentPreparationStatusRequest(
        @NotNull ShipmentStatus targetStatus
) {
}
