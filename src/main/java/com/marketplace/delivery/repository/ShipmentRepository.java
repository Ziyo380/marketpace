package com.marketplace.delivery.repository;

import com.marketplace.delivery.entity.Shipment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findBySellerOrder_Id(UUID sellerOrderId);

    @EntityGraph(attributePaths = "sellerOrder")
    List<Shipment> findAllBySellerOrder_Order_Id(UUID orderId);

    boolean existsBySellerOrder_Id(UUID sellerOrderId);
}
