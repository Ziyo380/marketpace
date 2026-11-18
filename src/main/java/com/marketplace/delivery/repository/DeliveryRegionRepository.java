package com.marketplace.delivery.repository;

import com.marketplace.delivery.entity.DeliveryRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRegionRepository extends JpaRepository<DeliveryRegion, String> {
}
