package com.marketplace.seller.repository;

import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
    boolean existsByUser_Id(UUID userId);


    Optional<Seller> findByUser_Id(UUID userId);

    List<Seller> findAllByStatus(SellerStatus status);
}
