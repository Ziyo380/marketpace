package com.marketplace.delivery.service;

import com.marketplace.delivery.dto.response.DeliveryRegionResponse;
import com.marketplace.delivery.entity.DeliveryRegion;
import com.marketplace.delivery.repository.DeliveryRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryRegionService {
    private final DeliveryRegionRepository deliveryRegionRepository;

    @Transactional(readOnly = true)
    public List<DeliveryRegionResponse> getAllRegions(){
        List<DeliveryRegion> regions =deliveryRegionRepository.findAll(Sort.by("name").ascending());
        return regions.stream()
                .map(region -> new DeliveryRegionResponse(
                        region.getCode(),
                        region.getName()
                ))
                .toList();

    }
}
