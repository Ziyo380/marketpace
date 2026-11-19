package com.marketplace.admin.service;

import com.marketplace.admin.dto.response.PlatformStatisticsResponse;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.mapper.ProductMapper;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.review.repository.ReviewRepository;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.dto.response.StoreResponse;
import com.marketplace.store.entity.Store;
import com.marketplace.store.mapper.StoreMapper;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.dto.response.UserProfileResponse;
import com.marketplace.user.entity.User;
import com.marketplace.user.entity.UserStatus;
import com.marketplace.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminManagementService {
    private final UserRepository users;
    private final SellerRepository sellers;
    private final StoreRepository stores;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final ReviewRepository reviews;
    private final StoreMapper storeMapper;
    private final ProductMapper productMapper;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PlatformStatisticsResponse statistics() {
        return new PlatformStatisticsResponse(users.count(), sellers.count(), stores.count(),
                products.count(), orders.count(), orders.countByStatusIn(java.util.List.of(OrderStatus.CONFIRMED, OrderStatus.IN_DELIVERY, OrderStatus.PARTIALLY_DELIVERED, OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED)),
                reviews.count());
    }

    @Transactional(readOnly = true)
    public Page<UserProfileResponse> users(Pageable pageable) {
        return users.findAll(pageable).map(user -> new UserProfileResponse(user.getId(),
                user.getUsername(), user.getStatus(), user.getFullName(), user.getPhone()));
    }

    @Transactional
    public UserProfileResponse changeUserStatus(String actorName, UUID id, UserStatus status) {
        User actor = actor(actorName);
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (actor.getId().equals(id) && status == UserStatus.BLOCKED) {
            throw new BusinessRuleException("Administrator cannot block own account");
        }
        user.changeStatus(status);
        audit.record(actor, status == UserStatus.BLOCKED ? "BLOCK_USER" : "UNBLOCK_USER",
                "USER", id, null);
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getStatus(),
                user.getFullName(), user.getPhone());
    }

    @Transactional(readOnly = true)
    public Page<StoreResponse> pendingStores(Pageable pageable) {
        return stores.findAllByApprovedFalseAndRejectionReasonIsNull(pageable).map(storeMapper::toResponse);
    }

    @Transactional
    public StoreResponse moderateStore(String actorName, UUID id, boolean approve, String reason) {
        User actor = actor(actorName);
        Store store = stores.findById(id).orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        store.setApproved(approve);
        store.setRejectionReason(approve ? null : requireReason(reason));
        audit.record(actor, approve ? "APPROVE_STORE" : "REJECT_STORE", "STORE", id,
                store.getRejectionReason());
        return storeMapper.toResponse(store);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> products(Pageable pageable) {
        return products.findAll(pageable).map(productMapper::toResponse);
    }

    @Transactional
    public ProductResponse moderateProduct(String actorName, UUID id, boolean block, String reason) {
        User actor = actor(actorName);
        Product product = products.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setModerationBlocked(block);
        if (block) product.setStatus(ProductStatus.ARCHIVED);
        audit.record(actor, block ? "BLOCK_PRODUCT" : "UNBLOCK_PRODUCT", "PRODUCT", id,
                block ? requireReason(reason) : null);
        return productMapper.toResponse(product);
    }

    private User actor(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Administrator not found"));
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) throw new BusinessRuleException("Reason is required");
        return reason.trim();
    }
}
