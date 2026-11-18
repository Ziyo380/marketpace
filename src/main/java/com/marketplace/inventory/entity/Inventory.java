package com.marketplace.inventory.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
public class Inventory extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
