package com.marketplace.product.mapper;

import com.marketplace.product.dto.response.ProductImageResponse;
import com.marketplace.product.entity.ProductImage;
import com.marketplace.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductImageMapper {

    private final ObjectStorageService objectStorageService;

    public ProductImageResponse toResponse(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getProduct().getId(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getSizeBytes(),
                objectStorageService.createReadUrl(image.getObjectKey()),
                image.isPrimary(),
                image.getSortOrder(),
                image.getCreatedAt()
        );
    }
}
