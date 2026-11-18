
package com.marketplace.inventory.repository;

import com.marketplace.inventory.entity.StockReservation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    Optional<StockReservation> findByOrderItem_Id(UUID orderItemId);

    List<StockReservation> findAllByOrderItem_SellerOrder_Id(UUID sellerOrderId);
}
