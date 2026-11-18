package com.marketplace.user.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name",  nullable = false, unique = true,  length = 20)
    private RoleName name;
}
