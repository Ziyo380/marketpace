package com.marketplace.cart.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,

        List<CartItemResponse>  items,

        int totalItems,

        BigDecimal totalPrice
) {
}
