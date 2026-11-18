package com.marketplace.product.entity;

import com.marketplace.category.entity.Category;
import com.marketplace.common.entity.BaseEntity;
import com.marketplace.store.entity.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
public class Product extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false,length = 200)
    private String name;

    @Column(nullable = false,length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private ProductStatus status=ProductStatus.DRAFT;

    @Column(name = "moderation_blocked", nullable = false)
    private boolean moderationBlocked;


    @Column(name = "price", nullable = true,precision = 19, scale = 2)
    private BigDecimal price;

}
