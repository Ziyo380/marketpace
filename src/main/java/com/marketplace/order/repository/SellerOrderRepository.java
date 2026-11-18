package com.marketplace.order.repository;

import com.marketplace.order.entity.SellerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerOrderRepository extends JpaRepository<SellerOrder, UUID> {
    Page<SellerOrder> findAllByStore_Id(UUID storeId, Pageable pageable);

    Optional<SellerOrder> findByIdAndStore_Id(UUID sellerOrderId, UUID storeId);

    List<SellerOrder> findAllByOrder_Id(UUID orderId);

    Page<SellerOrder> findAllByStore_Seller_User_Id(UUID storeId, Pageable pageable);

    Optional<SellerOrder> findByIdAndStore_Seller_User_Id(UUID sellerOrderId, UUID userId);
    @Query("select s.order.id from SellerOrder s where s.id = :sellerOrderId")
    Optional<UUID> findOrderId(@Param("sellerOrderId") UUID sellerOrderId);
}
