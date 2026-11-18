package com.marketplace.product.repository;

import com.marketplace.product.entity.ProductVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findAllByProduct_IdAndActiveTrueOrderByCreatedAtAsc(UUID productId);
    List<ProductVariant> findAllByProduct_IdOrderByCreatedAtAsc(UUID productId);
    Optional<ProductVariant> findByIdAndProduct_Id(UUID id, UUID productId);
    boolean existsByProduct_Id(UUID productId);
    boolean existsBySku(String sku);
}
