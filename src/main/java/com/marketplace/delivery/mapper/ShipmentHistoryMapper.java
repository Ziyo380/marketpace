package com.marketplace.delivery.mapper;

import com.marketplace.delivery.dto.response.ShipmentHistoryResponse;
import com.marketplace.delivery.entity.ShipmentStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class ShipmentHistoryMapper {
    public ShipmentHistoryResponse toResponse(ShipmentStatusHistory history) {
        return new ShipmentHistoryResponse(
                history.getId(), history.getShipment().getId(),
                history.getFromStatus(), history.getToStatus(),
                history.getActorType(), history.getActorUsername(),
                history.getChangedAt(), history.getDetails());
    }
}
