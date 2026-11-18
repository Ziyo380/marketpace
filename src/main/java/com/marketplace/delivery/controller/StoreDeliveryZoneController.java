package com.marketplace.delivery.controller;

import com.marketplace.delivery.dto.request.CreateStoreDeliveryZoneRequest;
import com.marketplace.delivery.dto.request.UpdateStoreDeliveryZoneStatusRequest;
import com.marketplace.delivery.dto.response.StoreDeliveryZoneResponse;
import com.marketplace.delivery.service.StoreDeliveryZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/stores/{storeId}/delivery-zones")
@RequiredArgsConstructor
public class StoreDeliveryZoneController {
    private final StoreDeliveryZoneService storeDeliveryZoneService;

    @PostMapping
    public ResponseEntity<StoreDeliveryZoneResponse> createZone(@PathVariable("storeId")UUID storeId, @Valid @RequestBody CreateStoreDeliveryZoneRequest request, Authentication authentication){
        StoreDeliveryZoneResponse response=storeDeliveryZoneService.createZone(authentication.getName(),storeId,request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StoreDeliveryZoneResponse>> getStoreDeliveryZones(@PathVariable("storeId")UUID storeId,Authentication authentication){
        List<StoreDeliveryZoneResponse>  response=storeDeliveryZoneService.getStoreDeliveryZones(authentication.getName(),storeId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{regionCode}/status")
    public ResponseEntity<StoreDeliveryZoneResponse> setZoneActive(@PathVariable UUID storeId, @PathVariable String regionCode, @Valid @RequestBody UpdateStoreDeliveryZoneStatusRequest request, Authentication authentication){
        StoreDeliveryZoneResponse response=storeDeliveryZoneService.setZoneActive(authentication.getName(),storeId,regionCode,request);
        return ResponseEntity.ok(response);
    }
}
