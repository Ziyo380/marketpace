package com.marketplace.inventory.controller;

import com.marketplace.inventory.dto.request.AddStockRequest;
import com.marketplace.inventory.dto.request.RemoveStockRequest;
import com.marketplace.inventory.dto.response.InventoryResponse;
import com.marketplace.inventory.dto.response.InventoryMovementResponse;
import com.marketplace.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class SellerInventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/products/{productId}/add-stock")
    public ResponseEntity<InventoryResponse> addStock(@PathVariable("productId") UUID productId, @Valid @RequestBody AddStockRequest request, Authentication authentication){
        InventoryResponse response=inventoryService.addStock(authentication.getName(),productId,request.amount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/{productId}/remove-stock")
    public ResponseEntity<InventoryResponse> removeStock(@PathVariable("productId") UUID productId, @Valid @RequestBody RemoveStockRequest request,Authentication authentication){
        InventoryResponse response=inventoryService.removeStock(authentication.getName(),productId,request.amount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<InventoryResponse> 	getInventoryByProductId(@PathVariable("productId") UUID productId, Authentication authentication){
        InventoryResponse response=inventoryService.getInventoryByProductId(authentication.getName(),productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/{productId}/movements")
    public ResponseEntity<List<InventoryMovementResponse>> getMovements(
            @PathVariable UUID productId, Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getMovements(authentication.getName(), productId));
    }

}
