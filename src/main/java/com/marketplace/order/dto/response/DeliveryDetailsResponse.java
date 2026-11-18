package com.marketplace.order.dto.response;

public record DeliveryDetailsResponse(
        String recipientName,
        String recipientPhone,
        String region,
        String city,
        String addressLine
) {
}
