package com.marketplace.seller.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sellers")
public class Seller extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SellerStatus status=SellerStatus.PENDING;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String rejectionReason;



}
