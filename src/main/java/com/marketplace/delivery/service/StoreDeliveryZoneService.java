package com.marketplace.delivery.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.delivery.dto.request.CreateStoreDeliveryZoneRequest;
import com.marketplace.delivery.dto.request.UpdateStoreDeliveryZoneRequest;
import com.marketplace.delivery.dto.request.UpdateStoreDeliveryZoneStatusRequest;
import com.marketplace.delivery.dto.response.StoreDeliveryZoneResponse;
import com.marketplace.delivery.entity.DeliveryRegion;
import com.marketplace.delivery.entity.StoreDeliveryZone;
import com.marketplace.delivery.mapper.StoreDeliveryZoneMapper;
import com.marketplace.delivery.repository.DeliveryRegionRepository;
import com.marketplace.delivery.repository.StoreDeliveryZoneRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreDeliveryZoneService {
    private final StoreDeliveryZoneRepository storeDeliveryZoneRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final DeliveryRegionRepository deliveryRegionRepository;
    private final StoreDeliveryZoneMapper  storeDeliveryZoneMapper;

    @Transactional
    public StoreDeliveryZoneResponse createZone(String username, UUID storeId, CreateStoreDeliveryZoneRequest request){
        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Store store=storeRepository.findById(storeId)
                .orElseThrow(()-> new ResourceNotFoundException("Store with id: "+storeId+" not found"));
        if (!store.getSeller().getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("Seller not found");
        }
        if (request==null){
            throw new BusinessRuleException("Invalid request");
        }else if (request.regionCode()==null || request.regionCode().isBlank()){
            throw new BusinessRuleException("Invalid request");
        }else if (request.deliveryFee()==null || request.deliveryFee().compareTo(BigDecimal.ZERO)<0){
            throw new BusinessRuleException("Invalid request");
        }else if (request.minDeliveryDays()==null || request.minDeliveryDays()<1){
            throw new BusinessRuleException("Invalid request");
        }else if (request.maxDeliveryDays()==null || request.maxDeliveryDays()<request.minDeliveryDays()){
            throw new BusinessRuleException("Invalid request");
        }
        DeliveryRegion region=deliveryRegionRepository.findById(request.regionCode().trim())
                .orElseThrow(()-> new ResourceNotFoundException("Region not found"));
        if (storeDeliveryZoneRepository.existsByStore_IdAndRegion_Code(store.getId(), region.getCode())){
            throw new ConflictException("Delivery zone already exists");
        }
        StoreDeliveryZone storeDeliveryZone=new StoreDeliveryZone();
        storeDeliveryZone.setStore(store);
        storeDeliveryZone.setRegion(region);
        storeDeliveryZone.setDeliveryFee(request.deliveryFee());
        storeDeliveryZone.setMinDeliveryDays(request.minDeliveryDays());
        storeDeliveryZone.setMaxDeliveryDays(request.maxDeliveryDays());
        storeDeliveryZone.setActive(true);
        StoreDeliveryZone savedStoreDeliveryZone=storeDeliveryZoneRepository.save(storeDeliveryZone);
        return storeDeliveryZoneMapper.toResponse(savedStoreDeliveryZone);


    }

    @Transactional(readOnly = true)
    public List<StoreDeliveryZoneResponse> getStoreDeliveryZones(String username, UUID storeId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Store store=storeRepository.findById(storeId)
                .orElseThrow(()-> new ResourceNotFoundException("Store with id: "+storeId+" not found"));
        if (!store.getSeller().getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("Seller not found");
        }
        List<StoreDeliveryZone> zones=storeDeliveryZoneRepository.findAllByStore_Id(store.getId());
        return zones.stream()
                .map(storeDeliveryZoneMapper::toResponse)
                .toList();

    }

    @Transactional
    public StoreDeliveryZoneResponse updateZone(String username, UUID storeId,String regionCode, UpdateStoreDeliveryZoneRequest request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Store store=storeRepository.findById(storeId)
                .orElseThrow(()-> new ResourceNotFoundException("Store with id: "+storeId+" not found"));
        if (!store.getSeller().getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("Store not found");
        }
        if (request==null){
            throw new BusinessRuleException("Invalid request");
        }else if (request.deliveryFee()==null || request.deliveryFee().compareTo(BigDecimal.ZERO)<0){
            throw new BusinessRuleException("Invalid request");
        }else if (request.minDeliveryDays()==null || request.minDeliveryDays()<1){
            throw new BusinessRuleException("Invalid request");
        }else if (request.maxDeliveryDays()==null || request.maxDeliveryDays()<request.minDeliveryDays()){
            throw new BusinessRuleException("Invalid request");
        }else if (regionCode==null || regionCode.trim().isBlank()){
            throw new BusinessRuleException("Invalid request");
        }
        StoreDeliveryZone zone=storeDeliveryZoneRepository.findByStore_IdAndRegion_Code(storeId,regionCode.trim())
                .orElseThrow(()-> new ResourceNotFoundException("Delivery zone not found"));
        zone.setDeliveryFee(request.deliveryFee());
        zone.setMinDeliveryDays(request.minDeliveryDays());
        zone.setMaxDeliveryDays(request.maxDeliveryDays());
        StoreDeliveryZone savedZone=storeDeliveryZoneRepository.save(zone);
        return storeDeliveryZoneMapper.toResponse(savedZone);
    }


    @Transactional
    public StoreDeliveryZoneResponse setZoneActive(String username, UUID storeId, String regionCode, UpdateStoreDeliveryZoneStatusRequest request){
        if (request==null){
            throw new BusinessRuleException("Invalid request");
        } else if (request.active()==null) {
            throw  new BusinessRuleException("Invalid request");
        } else if (regionCode==null || regionCode.trim().isBlank()){
            throw new BusinessRuleException("Invalid request");
        }
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        Store store=storeRepository.findById(storeId)
                .orElseThrow(()-> new ResourceNotFoundException("Store with id: "+storeId+" not found"));
        if (!store.getSeller().getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("Store not found");
        }
        StoreDeliveryZone zone=storeDeliveryZoneRepository.findByStore_IdAndRegion_Code(storeId,regionCode.trim())
                .orElseThrow(()-> new ResourceNotFoundException("Delivery zone not found"));
        if (request.active().equals(zone.isActive())){
            return storeDeliveryZoneMapper.toResponse(zone);
        }
        zone.setActive(request.active());
        StoreDeliveryZone savedZone=storeDeliveryZoneRepository.save(zone);
        return storeDeliveryZoneMapper.toResponse(savedZone);

    }
}
