package com.marketplace.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID itemId,
        UUID productId,
        UUID variantId,
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
