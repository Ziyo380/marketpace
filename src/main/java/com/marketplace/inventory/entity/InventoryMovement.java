package com.marketplace.inventory.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.product.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_movements")
@Getter @Setter @NoArgsConstructor
public class InventoryMovement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private InventoryMovementType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;
    @Column(name = "reserved_delta", nullable = false)
    private int reservedDelta;
    @Column(name = "quantity_after", nullable = false)
    private int quantityAfter;
    @Column(name = "reserved_after", nullable = false)
    private int reservedAfter;
}
