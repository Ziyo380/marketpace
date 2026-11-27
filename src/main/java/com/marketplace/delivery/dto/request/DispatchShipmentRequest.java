package com.marketplace.delivery.dto.request;

import jakarta.validation.constraints.Size;

public record DispatchShipmentRequest(
        @Size(max = 100) String carrierName,
        @Size(max = 128) String trackingNumber
) {
}
