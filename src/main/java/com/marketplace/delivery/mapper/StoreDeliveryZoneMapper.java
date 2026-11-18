package com.marketplace.delivery.mapper;

import com.marketplace.delivery.dto.response.StoreDeliveryZoneResponse;
import com.marketplace.delivery.entity.StoreDeliveryZone;
import org.springframework.stereotype.Component;

@Component
public class StoreDeliveryZoneMapper {
    public StoreDeliveryZoneResponse toResponse(StoreDeliveryZone zone){
        StoreDeliveryZoneResponse response=new StoreDeliveryZoneResponse(
                zone.getId(),
                zone.getStore().getId(),
                zone.getRegion().getCode(),
                zone.getRegion().getName(),
                zone.getDeliveryFee(),
                zone.getMinDeliveryDays(),
                zone.getMaxDeliveryDays(),
                zone.isActive()
        );
        return response;
    }
}
