package com.marketplace.promotion.dto.request;

import com.marketplace.promotion.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;

public record CreatePromocodeRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,40}") String code,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0") BigDecimal minimumAmount,
        Instant startsAt, Instant expiresAt, Integer maxUses) {}
