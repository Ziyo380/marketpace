package com.marketplace.product.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.entity.InventoryMovementType;
import com.marketplace.inventory.service.InventoryMovementService;
import com.marketplace.inventory.repository.InventoryMovementRepository;
import com.marketplace.inventory.dto.response.InventoryMovementResponse;
import com.marketplace.product.dto.request.CreateProductVariantRequest;
import com.marketplace.product.dto.request.UpdateProductVariantRequest;
import com.marketplace.product.dto.response.ProductVariantResponse;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.entity.ProductVariant;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.repository.ProductVariantRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductVariantService {
    private final ProductVariantRepository variants;
    private final ProductRepository products;
    private final UserRepository users;
    private final SellerRepository sellers;
    private final StoreRepository stores;
    private final InventoryMovementService movements;
    private final InventoryMovementRepository movementRepository;

    @Transactional
    public ProductVariantResponse create(String username, UUID productId, CreateProductVariantRequest request) {
        Product product = ownedProduct(username, productId);
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (variants.existsBySku(sku)) throw new ConflictException("SKU already exists");
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku(sku);
        variant.setName(request.name().trim());
        variant.setPrice(request.price());
        return response(variants.saveAndFlush(variant));
    }

    @Transactional
    public ProductVariantResponse update(String username, UUID productId, UUID variantId,
            UpdateProductVariantRequest request) {
        ownedProduct(username, productId);
        ProductVariant variant = variants.findByIdAndProduct_Id(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!request.active() && variant.getReservedQuantity() > 0) {
            throw new BusinessRuleException("Cannot deactivate a variant with reserved stock");
        }
        variant.setName(request.name().trim());
        variant.setPrice(request.price());
        variant.setActive(request.active());
        return response(variant);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> myVariants(String username, UUID productId) {
        ownedProduct(username, productId);
        return variants.findAllByProduct_IdOrderByCreatedAtAsc(productId).stream()
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> movements(String username, UUID productId, UUID variantId) {
        ownedVariant(username, productId, variantId);
        return movementRepository.findAllByVariant_IdOrderByCreatedAtAsc(variantId).stream()
                .map(item -> new InventoryMovementResponse(item.getId(), item.getType(),
                        item.getQuantityDelta(), item.getReservedDelta(), item.getQuantityAfter(),
                        item.getReservedAfter(), item.getCreatedAt())).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> publicVariants(UUID productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getStatus() != ProductStatus.PUBLISHED || product.isModerationBlocked()
                || !product.getStore().isActive() || !product.getStore().isApproved()
                || product.getStore().getSeller().getStatus() != SellerStatus.APPROVED
                || !product.getCategory().isActive()) {
            throw new ResourceNotFoundException("Product not available");
        }
        return variants.findAllByProduct_IdOrderByCreatedAtAsc(productId).stream()
                .map(this::response).toList();
    }

    @Transactional
    public ProductVariantResponse addStock(String username, UUID productId, UUID variantId, int amount) {
        ProductVariant variant = ownedVariant(username, productId, variantId);
        if (amount <= 0) throw new BusinessRuleException("Amount must be positive");
        try { variant.setQuantity(Math.addExact(variant.getQuantity(), amount)); }
        catch (ArithmeticException e) { throw new BusinessRuleException("Stock quantity is too large"); }
        movements.record(variant, InventoryMovementType.STOCK_ADDED, amount, 0);
        return response(variant);
    }

    @Transactional
    public ProductVariantResponse removeStock(String username, UUID productId, UUID variantId, int amount) {
        ProductVariant variant = ownedVariant(username, productId, variantId);
        if (amount <= 0 || amount > variant.getQuantity() - variant.getReservedQuantity()) {
            throw new BusinessRuleException("Amount exceeds available stock");
        }
        variant.setQuantity(variant.getQuantity() - amount);
        movements.record(variant, InventoryMovementType.STOCK_REMOVED, -amount, 0);
        return response(variant);
    }

    @Transactional
    public void reserve(UUID variantId, int amount) {
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!variant.isActive() || amount <= 0 || amount > variant.getQuantity() - variant.getReservedQuantity()) {
            throw new BusinessRuleException("Insufficient variant stock");
        }
        variant.setReservedQuantity(variant.getReservedQuantity() + amount);
        movements.record(variant, InventoryMovementType.RESERVED, 0, amount);
    }

    @Transactional
    public void release(UUID variantId, int amount) {
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (amount <= 0 || amount > variant.getReservedQuantity()) {
            throw new BusinessRuleException("Invalid variant reservation");
        }
        variant.setReservedQuantity(variant.getReservedQuantity() - amount);
        movements.record(variant, InventoryMovementType.RELEASED, 0, -amount);
    }

    @Transactional
    public void confirm(UUID variantId, int amount) {
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (amount <= 0 || amount > variant.getReservedQuantity()) {
            throw new BusinessRuleException("Invalid variant reservation");
        }
        variant.setQuantity(variant.getQuantity() - amount);
        variant.setReservedQuantity(variant.getReservedQuantity() - amount);
        movements.record(variant, InventoryMovementType.CONFIRMED, -amount, -amount);
    }

    private ProductVariant ownedVariant(String username, UUID productId, UUID variantId) {
        ownedProduct(username, productId);
        return variants.findByIdAndProduct_Id(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
    }

    private Product ownedProduct(String username, UUID productId) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Seller seller = sellers.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        Store store = stores.findBySeller_Id(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        return products.findByIdAndStore_Id(productId, store.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private ProductVariantResponse response(ProductVariant variant) {
        return new ProductVariantResponse(variant.getId(), variant.getProduct().getId(),
                variant.getSku(), variant.getName(), variant.getPrice(), variant.isActive(),
                variant.getQuantity(), variant.getReservedQuantity(),
                variant.getQuantity() - variant.getReservedQuantity());
    }
}
