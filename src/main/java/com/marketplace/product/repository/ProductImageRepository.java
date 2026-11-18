
package com.marketplace.product.repository;

import com.marketplace.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findAllByProduct_IdOrderBySortOrderAsc(UUID productId);

    Optional<ProductImage> findByIdAndProduct_Id(UUID imageId, UUID productId);
}
