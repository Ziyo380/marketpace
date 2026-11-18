package com.marketplace.delivery.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.store.entity.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "store_delivery_zones", uniqueConstraints =
        @UniqueConstraint(name = "uk_store_delivery_zones_store_region", columnNames = {"store_id", "region_code"}))
@Getter
@Setter
@NoArgsConstructor
public class StoreDeliveryZone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_code", nullable = false)
    private DeliveryRegion region;

    @Column(name = "delivery_fee", nullable = false,precision = 19,scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "min_delivery_days",nullable = false)
    private int minDeliveryDays;

    @Column(name = "max_delivery_days",nullable = false)
    private int maxDeliveryDays;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
