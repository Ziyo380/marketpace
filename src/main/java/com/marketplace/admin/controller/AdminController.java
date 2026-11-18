package com.marketplace.admin.controller;

import com.marketplace.admin.dto.request.ModerationReasonRequest;
import com.marketplace.admin.dto.response.AuditLogResponse;
import com.marketplace.admin.dto.response.PlatformStatisticsResponse;
import com.marketplace.admin.service.AdminManagementService;
import com.marketplace.admin.service.AuditService;
import com.marketplace.product.dto.response.ProductResponse;
import com.marketplace.store.dto.response.StoreResponse;
import com.marketplace.user.dto.response.UserProfileResponse;
import com.marketplace.user.entity.UserStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminManagementService service;
    private final AuditService audit;

    @GetMapping("/statistics")
    public PlatformStatisticsResponse statistics() { return service.statistics(); }
    @GetMapping("/users")
    public Page<UserProfileResponse> users(Pageable pageable) { return service.users(pageable); }
    @PatchMapping("/users/{id}/block")
    public UserProfileResponse blockUser(@PathVariable UUID id, Authentication authentication) {
        return service.changeUserStatus(authentication.getName(), id, UserStatus.BLOCKED);
    }
    @PatchMapping("/users/{id}/unblock")
    public UserProfileResponse unblockUser(@PathVariable UUID id, Authentication authentication) {
        return service.changeUserStatus(authentication.getName(), id, UserStatus.ACTIVE);
    }
    @GetMapping("/stores/pending")
    public Page<StoreResponse> pendingStores(Pageable pageable) { return service.pendingStores(pageable); }
    @PatchMapping("/stores/{id}/approve")
    public StoreResponse approveStore(@PathVariable UUID id, Authentication authentication) {
        return service.moderateStore(authentication.getName(), id, true, null);
    }
    @PatchMapping("/stores/{id}/reject")
    public StoreResponse rejectStore(@PathVariable UUID id, @Valid @RequestBody ModerationReasonRequest request,
            Authentication authentication) {
        return service.moderateStore(authentication.getName(), id, false, request.reason());
    }
    @GetMapping("/products")
    public Page<ProductResponse> products(Pageable pageable) { return service.products(pageable); }
    @PatchMapping("/products/{id}/block")
    public ProductResponse blockProduct(@PathVariable UUID id, @Valid @RequestBody ModerationReasonRequest request,
            Authentication authentication) {
        return service.moderateProduct(authentication.getName(), id, true, request.reason());
    }
    @PatchMapping("/products/{id}/unblock")
    public ProductResponse unblockProduct(@PathVariable UUID id, Authentication authentication) {
        return service.moderateProduct(authentication.getName(), id, false, null);
    }
    @GetMapping("/audit")
    public Page<AuditLogResponse> audit(Pageable pageable) { return audit.list(pageable); }
}
