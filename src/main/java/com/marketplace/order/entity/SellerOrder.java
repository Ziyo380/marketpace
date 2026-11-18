
package com.marketplace.order.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.store.entity.Store;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seller_orders")
@Getter
@Setter
@NoArgsConstructor
public class SellerOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SellerOrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "delivery_fee", precision = 19, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "delivery_min_days")
    private Integer minDeliveryDays;

    @Column(name = "delivery_max_days")
    private Integer maxDeliveryDays;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(
            mappedBy = "sellerOrder",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<OrderItem> items = new ArrayList<>();
}
