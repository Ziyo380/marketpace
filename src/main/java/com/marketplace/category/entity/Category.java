package com.marketplace.category.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
public class Category extends BaseEntity {
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "description",  length = 1000)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active=true;

    @JoinColumn(name = "parent_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;
}
