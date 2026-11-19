package com.marketplace.order.repository;

import com.marketplace.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndBuyer_Id(UUID orderId, UUID buyerId);
    Optional<Order> findByBuyer_IdAndIdempotencyKey(UUID buyerId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") UUID orderId);

    @Query("select o.id from Order o where o.expiresAt <= :now "
            + "and exists (select 1 from SellerOrder so where so.order = o and so.status = :pendingStatus)")
    List<UUID> findExpiredIds(@Param("now") Instant now,
            @Param("pendingStatus") com.marketplace.order.entity.SellerOrderStatus pendingStatus);

    Page<Order> findAllByBuyer_Id(UUID buyerId, Pageable pageable);
    long countByStatus(com.marketplace.order.entity.OrderStatus status);
    long countByStatusIn(List<com.marketplace.order.entity.OrderStatus> statuses);
}
