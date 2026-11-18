package com.marketplace.product.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "sku", nullable = false, unique = true, length = 80)
    private String sku;
    @Column(name = "name", nullable = false, length = 120)
    private String name;
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "quantity", nullable = false)
    private int quantity;
    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
