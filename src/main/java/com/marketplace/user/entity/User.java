package com.marketplace.user.entity;

import com.marketplace.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();


    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public void addRole(Role role) {
        Objects.requireNonNull(role, "Role must not be null");
        roles.add(role);
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public void changeStatus(UserStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }
}
