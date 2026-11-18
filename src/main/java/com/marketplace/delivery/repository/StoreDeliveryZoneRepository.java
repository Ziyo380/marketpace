package com.marketplace.delivery.repository;

import com.marketplace.delivery.entity.StoreDeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreDeliveryZoneRepository extends JpaRepository<StoreDeliveryZone, UUID> {
    List<StoreDeliveryZone> findAllByStore_Id(UUID storeId);

    Optional<StoreDeliveryZone> findByStore_IdAndRegion_Code(UUID storeId, String regionCode);

    boolean existsByStore_IdAndRegion_Code(UUID storeId, String regionCode);
}
