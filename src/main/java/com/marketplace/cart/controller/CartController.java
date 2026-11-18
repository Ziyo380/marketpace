package com.marketplace.cart.controller;

import com.marketplace.cart.dto.request.AddCartItemRequest;
import com.marketplace.cart.dto.request.UpdateCartItemRequest;
import com.marketplace.cart.dto.response.CartResponse;
import com.marketplace.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> findByUser_Id(Authentication authentication) {
        CartResponse response=cartService.getMyCart(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(Authentication authentication, @Valid @RequestBody AddCartItemRequest request) {
        CartResponse response=cartService.addItem(authentication.getName(),request.productId(),request.quantity(),request.variantId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(@PathVariable UUID itemId, Authentication authentication, @Valid @RequestBody UpdateCartItemRequest request) {
        CartResponse response=cartService.updateItem(authentication.getName(),itemId,request.quantity());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(Authentication authentication, @PathVariable UUID itemId) {
        CartResponse response=cartService.removeItem(authentication.getName(),itemId);
        return ResponseEntity.ok(response);
    }
}
