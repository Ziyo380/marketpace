package com.marketplace.order.mapper;

import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.dto.response.SellerOrderResponse;
import com.marketplace.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final SellerOrderMapper sellerOrderMapper;
    private final DeliveryDetailsMapper deliveryDetailsMapper;

    public OrderResponse toOrderResponse(Order order) {
        List<SellerOrderResponse> sellerOrders = order.getSellerOrders()
                .stream()
                .map(sellerOrderMapper::toSellerOrderResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                sellerOrders,
                order.getTotalAmount(),
                order.getCurrentTotalAmount(),
                order.getSubtotalAmount(),
                order.getDeliveryTotalAmount(),
                order.getDiscountAmount(),
                order.getPromocode() == null ? null : order.getPromocode().getCode(),
                deliveryDetailsMapper.toResponse(order),
                order.getCreatedAt()
        );
    }
}
