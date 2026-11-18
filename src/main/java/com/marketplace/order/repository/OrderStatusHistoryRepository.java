package com.marketplace.order.repository;

import com.marketplace.order.entity.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
    List<OrderStatusHistory> findAllByOrder_IdOrderByCreatedAtAsc(UUID orderId);
    List<OrderStatusHistory> findAllBySellerOrder_IdOrderByCreatedAtAsc(UUID sellerOrderId);
}
