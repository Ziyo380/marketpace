package com.marketplace.order.controller;

import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.dto.response.OrderHistoryResponse;
import com.marketplace.order.dto.request.CreateOrderRequest;
import jakarta.validation.Valid;
import com.marketplace.order.service.OrderCancellationService;
import com.marketplace.order.service.OrderCreationService;
import com.marketplace.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public  class CustomerOrderController {
    private final OrderService  orderService;
    private final OrderCreationService orderCreationService;
    private final OrderCancellationService orderCancellationService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getMyOrder(@PathVariable("orderId") UUID orderId, Authentication authentication){
        OrderResponse response=orderService.getMyOrder(authentication.getName(),orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderHistoryResponse>> getMyOrderHistory(
            @PathVariable UUID orderId, Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrderHistory(authentication.getName(), orderId));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(Authentication authentication, Pageable pageable){
        Page<OrderResponse> response=orderService.getMyOrders(authentication.getName(),pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request){
        OrderResponse response=orderCreationService.createOrder(authentication.getName(), idempotencyKey,
                request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelMyOrder(Authentication authentication, @PathVariable("orderId") UUID orderId){
        OrderResponse response=orderCancellationService.cancelMyOrder(authentication.getName(),orderId);
        return ResponseEntity.ok(response);
    }
}
