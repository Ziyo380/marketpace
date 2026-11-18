package com.marketplace.order.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.user.entity.User;
import com.marketplace.promotion.entity.Promocode;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "delivery_total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal deliveryTotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promocode_id")
    private Promocode promocode;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "delivery_region", length = 100)
    private String deliveryRegion;

    @Column(name = "delivery_city", length = 100)
    private String deliveryCity;

    @Column(name = "delivery_address_line", length = 500)
    private String deliveryAddressLine;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY,cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<OrderItem> items=new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    private List<SellerOrder> sellerOrders = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
