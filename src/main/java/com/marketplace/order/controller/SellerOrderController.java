package com.marketplace.order.controller;

import com.marketplace.order.dto.request.CancelOrderRequest;
import com.marketplace.order.dto.response.SellerOrderResponse;
import com.marketplace.order.dto.response.OrderHistoryResponse;
import com.marketplace.order.service.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {
    private final SellerOrderService sellerOrderService;


    @PatchMapping("/{sellerOrderId}/confirm")
    public ResponseEntity<SellerOrderResponse> confirmSellerOrder(@PathVariable("sellerOrderId") UUID sellerOrderId, Authentication  authentication){
        SellerOrderResponse response=sellerOrderService.confirmSellerOrder(authentication.getName(),sellerOrderId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{sellerOrderId}/cancel")
    public ResponseEntity<SellerOrderResponse> cancelSellerOrder(@PathVariable("sellerOrderId") UUID sellerOrderId, @Valid @RequestBody CancelOrderRequest request, Authentication  authentication){
        SellerOrderResponse response=sellerOrderService.cancelSellerOrder(authentication.getName(),sellerOrderId,request.reason());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<SellerOrderResponse>> getMySellerOrders(Authentication  authentication, Pageable pageable){
        Page<SellerOrderResponse> response=sellerOrderService.getMySellerOrders(authentication.getName(),pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sellerOrderId}")
    public ResponseEntity<SellerOrderResponse> getMySellerOrder(@PathVariable("sellerOrderId") UUID sellerOrderId, Authentication  authentication){
        SellerOrderResponse response=sellerOrderService.getMySellerOrder(authentication.getName(),sellerOrderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sellerOrderId}/history")
    public ResponseEntity<List<OrderHistoryResponse>> getMySellerOrderHistory(
            @PathVariable UUID sellerOrderId, Authentication authentication) {
        return ResponseEntity.ok(sellerOrderService.getMySellerOrderHistory(
                authentication.getName(), sellerOrderId));
    }
}
