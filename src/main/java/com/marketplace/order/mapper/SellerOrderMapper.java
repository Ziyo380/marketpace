package com.marketplace.order.mapper;

import com.marketplace.order.dto.response.OrderItemResponse;
import com.marketplace.order.dto.response.SellerOrderResponse;
import com.marketplace.order.entity.SellerOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SellerOrderMapper {
    private final OrderItemMapper orderItemMapper;
    private final DeliveryDetailsMapper deliveryDetailsMapper;

    public SellerOrderResponse toSellerOrderResponse(SellerOrder sellerOrder) {
        List<OrderItemResponse> items = sellerOrder.getItems()
                .stream()
                .map(orderItemMapper::toItemResponse)
                .toList();
        return new SellerOrderResponse(
                sellerOrder.getId(),
                sellerOrder.getOrder().getId(),
                sellerOrder.getStore().getId(),
                sellerOrder.getStatus(),
                items,
                sellerOrder.getTotalAmount(),
                sellerOrder.getDeliveryFee(),
                sellerOrder.getMinDeliveryDays(),
                sellerOrder.getMaxDeliveryDays(),
                sellerOrder.getConfirmedAt(),
                sellerOrder.getCancelledAt(),
                sellerOrder.getCancellationReason(),
                deliveryDetailsMapper.toResponse(sellerOrder.getOrder())
        );
    }
}
