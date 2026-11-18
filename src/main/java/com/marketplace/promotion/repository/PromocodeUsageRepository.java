package com.marketplace.promotion.repository;

import com.marketplace.promotion.entity.PromocodeUsage;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromocodeUsageRepository extends JpaRepository<PromocodeUsage, UUID> {
    boolean existsByUser_IdAndPromocode_Id(UUID userId, UUID promocodeId);
    Optional<PromocodeUsage> findByOrder_Id(UUID orderId);
}
