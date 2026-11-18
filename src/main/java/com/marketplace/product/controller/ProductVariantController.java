package com.marketplace.product.controller;

import com.marketplace.product.dto.request.CreateProductVariantRequest;
import com.marketplace.product.dto.request.UpdateProductVariantRequest;
import com.marketplace.product.dto.request.VariantStockRequest;
import com.marketplace.product.dto.response.ProductVariantResponse;
import com.marketplace.product.service.ProductVariantService;
import com.marketplace.inventory.dto.response.InventoryMovementResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductVariantController {
    private final ProductVariantService service;

    @GetMapping("/{productId}/variants")
    public List<ProductVariantResponse> publicVariants(@PathVariable UUID productId) {
        return service.publicVariants(productId);
    }
    @GetMapping("/me/{productId}/variants")
    public List<ProductVariantResponse> myVariants(@PathVariable UUID productId, Authentication authentication) {
        return service.myVariants(authentication.getName(), productId);
    }
    @PostMapping("/me/{productId}/variants")
    public ResponseEntity<ProductVariantResponse> create(@PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(authentication.getName(), productId, request));
    }
    @PutMapping("/me/{productId}/variants/{variantId}")
    public ProductVariantResponse update(@PathVariable UUID productId, @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request, Authentication authentication) {
        return service.update(authentication.getName(), productId, variantId, request);
    }
    @PostMapping("/me/{productId}/variants/{variantId}/add-stock")
    public ProductVariantResponse addStock(@PathVariable UUID productId, @PathVariable UUID variantId,
            @Valid @RequestBody VariantStockRequest request, Authentication authentication) {
        return service.addStock(authentication.getName(), productId, variantId, request.amount());
    }
    @PostMapping("/me/{productId}/variants/{variantId}/remove-stock")
    public ProductVariantResponse removeStock(@PathVariable UUID productId, @PathVariable UUID variantId,
            @Valid @RequestBody VariantStockRequest request, Authentication authentication) {
        return service.removeStock(authentication.getName(), productId, variantId, request.amount());
    }
    @GetMapping("/me/{productId}/variants/{variantId}/movements")
    public List<InventoryMovementResponse> movements(@PathVariable UUID productId,
            @PathVariable UUID variantId, Authentication authentication) {
        return service.movements(authentication.getName(), productId, variantId);
    }
}
