package com.marketplace.store.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, unique = true, updatable = false)
    private Seller seller;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false,unique = true, length = 180)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active=true;

    @Column(name = "approved", nullable = false)
    private boolean approved = true;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

}
