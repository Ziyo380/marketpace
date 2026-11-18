package com.marketplace.order.mapper;

import com.marketplace.order.dto.response.DeliveryDetailsResponse;
import com.marketplace.order.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class DeliveryDetailsMapper {
    public DeliveryDetailsResponse toResponse(Order order) {
        if (order.getRecipientName() == null) {
            return null;
        }
        return new DeliveryDetailsResponse(
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getDeliveryRegion(),
                order.getDeliveryCity(),
                order.getDeliveryAddressLine()
        );
    }
}
