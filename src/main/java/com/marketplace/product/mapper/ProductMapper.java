package com.marketplace.product.mapper;

import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductResponse toResponse(Product product) {
        ProductResponse response=new ProductResponse(
                product.getId(),
                product.getStore().getId(),
                product.getStore().getSlug(),
                product.getCategory().getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getPrice(),
                product.isModerationBlocked()

        );
        return response;
    }
}
