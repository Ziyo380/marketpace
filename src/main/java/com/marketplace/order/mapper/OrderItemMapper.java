package com.marketplace.order.mapper;

import com.marketplace.order.dto.response.OrderItemResponse;
import com.marketplace.order.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemResponse toItemResponse(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProduct().getId(),
                orderItem.getVariant() == null ? null : orderItem.getVariant().getId(),
                orderItem.getSku(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getSubtotal()
        );
    }
}
