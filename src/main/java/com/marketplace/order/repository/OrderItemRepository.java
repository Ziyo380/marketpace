package com.marketplace.order.repository;

import com.marketplace.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.marketplace.order.entity.SellerOrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findAllByOrder_Id(UUID orderId);

    Optional<OrderItem> findByOrder_IdAndProduct_Id(UUID orderId, UUID productId);

    @Query("select count(i) > 0 from OrderItem i where i.order.buyer.id = :userId "
            + "and i.product.id = :productId and i.sellerOrder.status = :status")
    boolean hasPurchaseWithStatus(@Param("userId") UUID userId,
            @Param("productId") UUID productId, @Param("status") SellerOrderStatus status);

}
