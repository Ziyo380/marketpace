package com.marketplace.delivery.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.delivery.entity.StoreDeliveryZone;
import com.marketplace.delivery.repository.DeliveryRegionRepository;
import com.marketplace.delivery.repository.StoreDeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeliveryAvailabilityService {
    private final DeliveryRegionRepository deliveryRegionRepository;
    private final StoreDeliveryZoneRepository storeDeliveryZoneRepository;

    @Transactional(readOnly = true)
    public Map<UUID, StoreDeliveryZone> validateDelivery(String regionCode, Set<UUID> storeIds){
        if (regionCode == null || regionCode.isBlank()){
            throw new BusinessRuleException("RegionCode cannot be null or empty");
        }
        String normalizedRegionCode = regionCode.trim();
        if (storeIds == null || storeIds.isEmpty() ||  storeIds.contains(null)){
            throw new BusinessRuleException("Store Ids cannot be null or empty");
        }
        if (!deliveryRegionRepository.existsById(normalizedRegionCode)){
            throw new ResourceNotFoundException("Delivery region not found");
        }
        Map<UUID, StoreDeliveryZone> deliveryZones = new HashMap<>();
        for (UUID storeId : storeIds){
            StoreDeliveryZone zone = storeDeliveryZoneRepository.findByStore_IdAndRegion_Code(storeId,normalizedRegionCode)
                    .orElseThrow(()-> new BusinessRuleException("Zone not found"));
            if (!zone.isActive()){
                throw new BusinessRuleException("Store delivery is unavailable for selected region");
            }
            deliveryZones.put(storeId, zone);
        }
        return deliveryZones;
    }
}
