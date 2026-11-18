package com.marketplace.favorite.repository;

import com.marketplace.favorite.entity.Favorite;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    Page<Favorite> findAllByUser_Id(UUID userId, Pageable pageable);
    Optional<Favorite> findByUser_IdAndProduct_Id(UUID userId, UUID productId);
}
