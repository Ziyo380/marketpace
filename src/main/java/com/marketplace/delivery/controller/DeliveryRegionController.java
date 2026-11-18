package com.marketplace.delivery.controller;

import com.marketplace.delivery.dto.response.DeliveryRegionResponse;
import com.marketplace.delivery.service.DeliveryRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delivery/regions")
@RequiredArgsConstructor
public class DeliveryRegionController {
    private final DeliveryRegionService deliveryRegionService;

    @GetMapping
    public ResponseEntity<List<DeliveryRegionResponse>> findAll() {
        List<DeliveryRegionResponse> response=deliveryRegionService.getAllRegions();
        return ResponseEntity.ok(response);
    }
}
