package com.marketplace.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID itemId,

        UUID productId,

        UUID variantId,

        String sku,

        String productName,

        BigDecimal unitPrice,

        int quantity,

        BigDecimal subtotal
) {
}
