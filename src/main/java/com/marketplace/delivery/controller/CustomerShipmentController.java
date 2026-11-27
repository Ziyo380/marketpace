package com.marketplace.delivery.controller;

import com.marketplace.delivery.dto.response.ShipmentResponse;
import com.marketplace.delivery.dto.response.ShipmentHistoryResponse;
import com.marketplace.delivery.service.ShipmentHistoryService;
import com.marketplace.delivery.mapper.ShipmentMapper;
import com.marketplace.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class CustomerShipmentController {
    private final ShipmentService shipmentService;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentHistoryService shipmentHistoryService;

    @GetMapping("/{orderId}/shipments/{shipmentId}/history")
    public List<ShipmentHistoryResponse> getCustomerHistory(
            @PathVariable("orderId") UUID orderId,
            @PathVariable("shipmentId") UUID shipmentId,
            Authentication authentication) {
        return shipmentHistoryService.getCustomerHistory(authentication.getName(), orderId, shipmentId);
    }

    @GetMapping("/{orderId}/shipments")
    public List<ShipmentResponse> getCustomerShipments(
            @PathVariable("orderId") UUID orderId,
            Authentication authentication) {
        return shipmentService.getCustomerShipments(authentication.getName(), orderId)
                .stream()
                .map(shipmentMapper::toResponse)
                .toList();
    }
}
