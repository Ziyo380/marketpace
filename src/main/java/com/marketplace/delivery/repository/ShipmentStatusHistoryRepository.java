package com.marketplace.delivery.repository;

import com.marketplace.delivery.entity.ShipmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, UUID> {
    @Query("select h from ShipmentStatusHistory h where h.shipment.id = :shipmentId order by h.changedAt asc, h.id asc")
    List<ShipmentStatusHistory> findAllByShipment_IdOrderByChangedAtAsc(@Param("shipmentId") UUID shipmentId);
}
