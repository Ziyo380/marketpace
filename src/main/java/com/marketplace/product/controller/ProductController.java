package com.marketplace.product.controller;

import com.marketplace.product.dto.request.CreateProductRequest;
import com.marketplace.product.dto.request.ReorderProductImagesRequest;
import com.marketplace.product.dto.request.UpdateProductRequest;
import com.marketplace.product.dto.response.ProductImageResponse;
import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.service.ProductImageService;
import com.marketplace.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductImageService productImageService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequest request, Authentication authentication) {
        ProductResponse response=productService.createProduct(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(Authentication authentication, Pageable pageable) {
        Page<ProductResponse> responses=productService.getMyProducts(authentication.getName(), pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/me/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID productId, Authentication authentication) {
        ProductResponse response=productService.getMyProductById(authentication.getName(), productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@RequestBody @Valid UpdateProductRequest request, Authentication authentication, @PathVariable UUID productId) {
        ProductResponse response=productService.updateProduct(authentication.getName(),productId,request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/{productId}/publish")
    public ResponseEntity<ProductResponse> publishProduct(@PathVariable UUID productId,Authentication authentication) {
        ProductResponse response=productService.publishMyProduct(authentication.getName(), productId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/{productId}/archive")
    public ResponseEntity<ProductResponse> archiveProduct(@PathVariable UUID productId,Authentication authentication) {
        ProductResponse response=productService.archiveMyProduct(authentication.getName(), productId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/{productId}/restore")
    public ResponseEntity<ProductResponse> restoreProduct(@PathVariable UUID productId,Authentication authentication) {
        ProductResponse response=productService.restoreMyProduct(authentication.getName(), productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String storeSlug,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            Pageable pageable) {
        Page<ProductResponse> responses=productService.getPublicProducts(categoryId, search,
                minPrice, maxPrice, storeSlug, availableOnly, pageable);
        return ResponseEntity.ok(responses);
    }

    @PostMapping(value = "/me/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponse> uploadProductImage(
            @PathVariable UUID productId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        ProductImageResponse response = productImageService.uploadImage(
                authentication.getName(), productId, file
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/{productId}/images")
    public ResponseEntity<List<ProductImageResponse>> getMyProductImages(
            @PathVariable UUID productId,
            Authentication authentication) {
        return ResponseEntity.ok(
                productImageService.getMyProductImages(authentication.getName(), productId)
        );
    }

    @PatchMapping("/me/{productId}/images/{imageId}/primary")
    public ResponseEntity<ProductImageResponse> setPrimaryProductImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            Authentication authentication) {
        return ResponseEntity.ok(
                productImageService.setPrimaryImage(authentication.getName(), productId, imageId)
        );
    }

    @PutMapping("/me/{productId}/images/order")
    public ResponseEntity<List<ProductImageResponse>> reorderProductImages(
            @PathVariable UUID productId,
            @RequestBody @Valid ReorderProductImagesRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                productImageService.reorderImages(authentication.getName(), productId, request)
        );
    }

    @DeleteMapping("/me/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            Authentication authentication) {
        productImageService.deleteImage(authentication.getName(), productId, imageId);
        return ResponseEntity.noContent().build();
    }


}
