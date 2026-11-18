package com.marketplace.product.repository;

import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.seller.entity.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product>{
    boolean existsByStore_IdAndSlug(UUID storeId, String slug);

    Optional<Product> findByStore_IdAndSlug(UUID storeId, String slug);

    Page<Product> findAllByStore_Id(UUID storeId, Pageable pageable);
    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    Optional<Product> findByIdAndStore_Id(UUID productId, UUID storeId);

    Page<Product> findAllByStore_IdAndStatusAndModerationBlockedFalseAndCategory_ActiveTrue(
            UUID storeId,
            ProductStatus status,
            Pageable pageable
    );

    Page<Product> findAllByStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(ProductStatus status, SellerStatus sellerStatus, Pageable pageable);

    Page<Product> findAllByCategory_IdAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByNameContainingIgnoreCaseAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            String search,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndNameContainingIgnoreCaseAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            String search,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            BigDecimal minPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            BigDecimal minPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByNameContainingIgnoreCaseAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByNameContainingIgnoreCaseAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            String search,
            BigDecimal minPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByNameContainingIgnoreCaseAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            String search,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceBetweenAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            String search,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceGreaterThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            String search,
            BigDecimal minPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );

    Page<Product> findAllByCategory_IdAndNameContainingIgnoreCaseAndPriceLessThanEqualAndStatusAndModerationBlockedFalseAndStore_ActiveTrueAndStore_ApprovedTrueAndStore_Seller_StatusAndCategory_ActiveTrue(
            UUID categoryId,
            String search,
            BigDecimal maxPrice,
            ProductStatus status,
            SellerStatus sellerStatus,
            Pageable pageable
    );
}


