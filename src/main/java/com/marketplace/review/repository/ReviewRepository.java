package com.marketplace.review.repository;

import com.marketplace.review.entity.Review;
import com.marketplace.review.entity.ReviewStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByUser_IdAndProduct_Id(UUID userId, UUID productId);
    Optional<Review> findByIdAndUser_Id(UUID reviewId, UUID userId);
    Page<Review> findAllByProduct_IdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);
    Page<Review> findAllByUser_Id(UUID userId, Pageable pageable);
    Page<Review> findAllByStatus(ReviewStatus status, Pageable pageable);
    long countByProduct_IdAndStatus(UUID productId, ReviewStatus status);
    @Query("select avg(r.rating) from Review r where r.product.id = :productId and r.status = :status")
    Double averageRating(@Param("productId") UUID productId, @Param("status") ReviewStatus status);
}
