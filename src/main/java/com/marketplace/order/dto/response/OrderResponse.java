package com.marketplace.order.dto.response;

import com.marketplace.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,

        OrderStatus status,

        List<SellerOrderResponse> sellerOrders,

        BigDecimal totalAmount,

        BigDecimal subtotalAmount,

        BigDecimal deliveryTotalAmount,

        BigDecimal discountAmount,

        String promocode,
        DeliveryDetailsResponse delivery,

        Instant createdAt
) {
}
