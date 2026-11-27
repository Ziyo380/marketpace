package com.marketplace.delivery.controller;

import com.marketplace.delivery.dto.request.DispatchShipmentRequest;
import com.marketplace.delivery.dto.request.MarkDeliveryFailedRequest;
import com.marketplace.delivery.dto.request.UpdateShipmentPreparationStatusRequest;
import com.marketplace.delivery.dto.response.ShipmentResponse;
import com.marketplace.delivery.dto.response.ShipmentHistoryResponse;
import com.marketplace.delivery.service.ShipmentHistoryService;
import com.marketplace.delivery.entity.Shipment;
import com.marketplace.delivery.mapper.ShipmentMapper;
import com.marketplace.delivery.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/orders/{sellerOrderId}/shipment")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;
    private final ShipmentMapper shipmentMapper;
    private final ShipmentHistoryService shipmentHistoryService;

    @GetMapping("/history")
    public List<ShipmentHistoryResponse> getSellerHistory(
            @PathVariable("sellerOrderId") UUID sellerOrderId, Authentication authentication) {
        return shipmentHistoryService.getSellerHistory(authentication.getName(), sellerOrderId);
    }

    @GetMapping
    public ShipmentResponse getSellerShipment(
            @PathVariable("sellerOrderId") UUID sellerOrderId,
            Authentication authentication) {
        Shipment shipment = shipmentService.getSellerShipment(authentication.getName(), sellerOrderId);
        return shipmentMapper.toResponse(shipment);
    }

    @PatchMapping
    public ShipmentResponse updatePreparationStatus(
            @PathVariable("sellerOrderId") UUID sellerOrderId,
            @Valid @RequestBody UpdateShipmentPreparationStatusRequest request,
            Authentication authentication) {
        Shipment shipment = shipmentService.updatePreparationStatus(
                authentication.getName(), sellerOrderId, request.targetStatus());
        return shipmentMapper.toResponse(shipment);
    }

    @PostMapping("/dispatch")
    public ShipmentResponse dispatchShipment(
            @PathVariable("sellerOrderId") UUID sellerOrderId,
            @Valid @RequestBody DispatchShipmentRequest request,
            Authentication authentication) {
        Shipment shipment = shipmentService.dispatchShipment(
                authentication.getName(), sellerOrderId,
                request.carrierName(), request.trackingNumber());
        return shipmentMapper.toResponse(shipment);
    }

    @PostMapping("/confirm-delivery")
    public ShipmentResponse confirmDelivery(
            @PathVariable("sellerOrderId") UUID sellerOrderId,
            Authentication authentication) {
        Shipment shipment = shipmentService.confirmDelivery(authentication.getName(), sellerOrderId);
        return shipmentMapper.toResponse(shipment);
    }

    @PostMapping("/delivery-failed")
    public ShipmentResponse markDeliveryFailed(
            @PathVariable("sellerOrderId") UUID sellerOrderId,
            @Valid @RequestBody MarkDeliveryFailedRequest request,
            Authentication authentication) {
        Shipment shipment = shipmentService.markDeliveryFailed(
                authentication.getName(), sellerOrderId, request.failureReason());
        return shipmentMapper.toResponse(shipment);
    }
}
