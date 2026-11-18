package com.marketplace.store.repository;

import com.marketplace.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.marketplace.seller.entity.SellerStatus;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository  extends JpaRepository<Store, UUID> {
    boolean existsBySeller_Id(UUID seller_id);

    boolean existsBySlug(String slug);

    Optional<Store> findBySlug(String slug);

    Optional<Store> findBySeller_Id(UUID sellerId);
    Page<Store> findAllByActiveTrueAndApprovedTrueAndSeller_Status(SellerStatus status, Pageable pageable);
    Page<Store> findAllByApprovedFalseAndRejectionReasonIsNull(Pageable pageable);
}
