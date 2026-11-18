package com.marketplace.cart.repository;

import com.marketplace.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCart_IdAndProduct_Id(UUID cart_Id,UUID product_Id);

    @Query("select c from CartItem c where c.cart.id = :cartId and c.product.id = :productId "
            + "and ((:variantId is null and c.variant is null) or c.variant.id = :variantId)")
    Optional<CartItem> findByCartProductAndVariant(@Param("cartId") UUID cartId,
            @Param("productId") UUID productId, @Param("variantId") UUID variantId);

    List<CartItem> findByCart_Id(UUID cart_Id);

    boolean existsByCart_IdAndProduct_Id(UUID cart_Id,UUID product_Id);

    void deleteByCart_IdAndProduct_Id(UUID cart_Id,UUID product_Id);

    Optional<CartItem> findByIdAndCart_Id(UUID itemId, UUID cartId);
}
