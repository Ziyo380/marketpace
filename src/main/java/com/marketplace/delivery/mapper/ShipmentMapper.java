package com.marketplace.delivery.mapper;

import com.marketplace.delivery.dto.response.ShipmentResponse;
import com.marketplace.delivery.entity.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {
    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getSellerOrder().getId(),
                shipment.getStatus(),
                shipment.getCarrierName(),
                shipment.getTrackingNumber(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getFailureReason()
        );
    }
}
