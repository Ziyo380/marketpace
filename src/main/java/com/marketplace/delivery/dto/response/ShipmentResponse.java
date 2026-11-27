package com.marketplace.delivery.dto.response;

import com.marketplace.delivery.entity.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID sellerOrderId,
        ShipmentStatus status,
        String carrierName,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt,
        String failureReason
){
}
