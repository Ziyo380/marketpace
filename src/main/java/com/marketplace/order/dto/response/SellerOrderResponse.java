package com.marketplace.order.dto.response;

import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.seller.entity.SellerStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellerOrderResponse(
        UUID sellerOrderId,

        UUID orderId,

        UUID storeId,

        SellerOrderStatus status,

        List<OrderItemResponse> items,

        BigDecimal totalAmount,

        BigDecimal discountAmount,

        BigDecimal payableAmount,

        BigDecimal deliveryFee,

        Integer minDeliveryDays,

        Integer maxDeliveryDays,

        Instant confirmedAt,

        Instant cancelledAt,

        String cancellationReson,
        DeliveryDetailsResponse delivery

) {
}
