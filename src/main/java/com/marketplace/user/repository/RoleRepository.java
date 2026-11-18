package com.marketplace.user.repository;

import com.marketplace.user.entity.Role;
import com.marketplace.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface RoleRepository extends JpaRepository<Role, UUID>{
    Optional<Role> findByName(RoleName name);
}
