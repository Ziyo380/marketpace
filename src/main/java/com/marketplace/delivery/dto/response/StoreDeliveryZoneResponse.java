package com.marketplace.delivery.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreDeliveryZoneResponse(
        UUID id,
        UUID storeId,
        String regionCode,
        String regionName,
        BigDecimal deliveryFee,
        int minDeliveryDays,
        int maxDeliveryDays,
        boolean active
) {
}
