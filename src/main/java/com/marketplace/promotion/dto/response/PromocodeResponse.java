package com.marketplace.promotion.dto.response;

import com.marketplace.promotion.entity.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromocodeResponse(UUID id, String code, DiscountType discountType,
        BigDecimal discountValue, BigDecimal minimumAmount, Instant startsAt,
        Instant expiresAt, Integer maxUses, int usedCount, boolean active) {}
