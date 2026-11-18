package com.marketplace.promotion.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promocodes")
@Getter @Setter @NoArgsConstructor
public class Promocode extends BaseEntity {
    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 16)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "minimum_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumAmount = BigDecimal.ZERO;
    @Column(name = "starts_at")
    private Instant startsAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "max_uses")
    private Integer maxUses;
    @Column(name = "used_count", nullable = false)
    private int usedCount;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
