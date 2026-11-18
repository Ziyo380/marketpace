package com.marketplace.product.controller;

import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.dto.response.ProductImageResponse;
import com.marketplace.product.service.ProductImageService;
import com.marketplace.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class PublicProductController {
    private final ProductService productService;
    private final ProductImageService productImageService;

    @GetMapping("/{storeSlug}/products")
    public ResponseEntity<Page<ProductResponse>> getPublicProductsByStoreSlug(
            @PathVariable String storeSlug,
            Pageable pageable){
        Page<ProductResponse> responses=productService.getPublicProductsByStoreSlug(storeSlug, pageable);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{storeSlug}/products/{productSlug}")
    public ResponseEntity<ProductResponse> getPublicProductBySlugs(@PathVariable String storeSlug, @PathVariable String productSlug){
        ProductResponse response=productService.getPublicProductBySlugs(storeSlug,productSlug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{storeSlug}/products/{productSlug}/images")
    public ResponseEntity<List<ProductImageResponse>> getPublicProductImages(
            @PathVariable String storeSlug,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(
                productImageService.getPublicProductImages(storeSlug, productSlug)
        );
    }



}
