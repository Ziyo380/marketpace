package com.marketplace.cart.entity;

import com.marketplace.common.entity.BaseEntity;
import com.marketplace.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "carts")
@Getter @Setter
@NoArgsConstructor
public class Cart extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    private List<CartItem> items=new ArrayList<>();
}
