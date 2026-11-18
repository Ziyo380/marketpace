package com.marketplace.review.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.product.entity.Product;
import com.marketplace.user.entity.User;
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
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor
public class Review extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "rating", nullable = false)
    private int rating;
    @Column(name = "comment", length = 2000)
    private String comment;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;
}
