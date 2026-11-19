package com.marketplace.cart.service;

import com.marketplace.cart.dto.response.CartResponse;
import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.cart.mapper.CartMapper;
import com.marketplace.cart.repository.CartItemRepository;
import com.marketplace.cart.repository.CartRepository;
import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.repository.InventoryRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductVariant;
import com.marketplace.product.repository.ProductVariantRepository;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService{
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public CartResponse getMyCart(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User with username "+username+" not found"));
        Cart cart=cartRepository.findByUser_Id(user.getId())
                .orElseGet(()->{
                    Cart newCart=new Cart();
                newCart.setUser(user);
                return cartRepository.save(newCart);});

        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse  addItem(String username, UUID productId, int quantity){
        return addItem(username, productId, quantity, null);
    }

    @Transactional
    public CartResponse addItem(String username, UUID productId, int quantity, UUID variantId){
        if (quantity<=0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity should be greater than 0");
        }
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User with username "+username+" not found"));
        Cart cart=cartRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(()->{
                    Cart newCart=new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
        Product product=productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product with id "+productId+" not found"));
        if (!product.getStatus().equals(ProductStatus.PUBLISHED)){
            throw new BusinessRuleException("Product with id "+productId+" is not published");
        }
        if (product.isModerationBlocked()) {
            throw new BusinessRuleException("Product is not available");
        }
        if (!product.getCategory().isActive()) {
            throw new BusinessRuleException("Product category must be ACTIVE");
        }
        Store store=product.getStore();
        if (!store.isActive() || !store.isApproved()){
            throw new BusinessRuleException("Store with id "+store.getId()+" is not active");
        }

        Seller seller=store.getSeller();
        if (seller.getStatus()!=(SellerStatus.APPROVED)){
            throw new BusinessRuleException("Seller with id "+seller.getId()+" is not approved");
        }
        ProductVariant variant = null;
        if (variantId != null) {
            variant = variantRepository.findByIdAndProduct_Id(variantId, productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
            if (!variant.isActive()) throw new BusinessRuleException("Variant is inactive");
        } else if (variantRepository.existsByProduct_Id(productId)) {
            throw new BusinessRuleException("Select a variant before adding this product");
        }
        Optional<CartItem> existingItem=cartItemRepository.findByCartProductAndVariant(cart.getId(),productId,variantId);
        int currentQuantity=0;
        if (existingItem.isPresent()){
            currentQuantity=existingItem.get().getQuantity();
        }
        int newQuantity;

        try {
            newQuantity=Math.addExact(currentQuantity,quantity);
        }catch (ArithmeticException e){
            throw new BusinessRuleException("Quantity exceeds the maximum allowed value");
        }

        int availableQuantity;
        if (variant != null) {
            availableQuantity = variant.getQuantity() - variant.getReservedQuantity();
        } else {
            Inventory inventory=inventoryRepository.findByProduct_Id(productId)
                    .orElseThrow(()->new ResourceNotFoundException("Product with id "+productId+" not found"));
            availableQuantity=inventory.getQuantity()-inventory.getReservedQuantity();
        }
        if (newQuantity>availableQuantity){
            throw new BusinessRuleException("Quantity should be less than availableQuantity");
        }
        CartItem cartItem;

        if (existingItem.isPresent()){
            cartItem=existingItem.get();
            cartItem.setQuantity(newQuantity);
        }else  {
            cartItem=new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setVariant(variant);
            cartItem.setQuantity(quantity);
            cart.getItems().add(cartItem);
        }
        cartItemRepository.save(cartItem);
        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(String username, UUID itemId, int quantity){
        if (quantity<=0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity should be greater than 0");
        }
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User with username "+username+" not found"));
        Cart cart=cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(()->new ResourceNotFoundException("Cart not found for user "+username));
        CartItem cartItem=cartItemRepository.findByIdAndCart_Id(itemId,cart.getId())
                .orElseThrow(()->new ResourceNotFoundException("Item with id "+itemId+" not found"));
        Product product=cartItem.getProduct();
        if (product.getStatus() != ProductStatus.PUBLISHED){
            throw new BusinessRuleException("Product with id "+product.getId()+" is not published");
        }
        if (product.isModerationBlocked() || !product.getCategory().isActive()) {
            throw new BusinessRuleException("Product is not available");
        }
        if (!product.getStore().isActive() || !product.getStore().isApproved()){
            throw new BusinessRuleException("Store with id "+product.getStore().getId()+" is not active");
        }
        if (product.getStore().getSeller().getStatus()!=SellerStatus.APPROVED){
            throw new BusinessRuleException("Seller with id "+product.getStore().getSeller().getId()+" is not approved");
        }
        int availableQuantity;
        if (cartItem.getVariant() != null) {
            ProductVariant variant = cartItem.getVariant();
            if (!variant.isActive()) throw new BusinessRuleException("Variant is inactive");
            availableQuantity = variant.getQuantity() - variant.getReservedQuantity();
        } else {
            Inventory inventory=inventoryRepository.findByProduct_Id(product.getId())
                    .orElseThrow(()->new ResourceNotFoundException("Product with id "+product.getId()+" not found"));
            availableQuantity=inventory.getQuantity()-inventory.getReservedQuantity();
        }
        if (availableQuantity<quantity){
            throw new BusinessRuleException("Quantity should be less than availableQuantity");
        }
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String username, UUID itemId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()->new ResourceNotFoundException("User with username "+username+" not found"));
        Cart cart=cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(()->new ResourceNotFoundException("Cart not found for user "+username));
        CartItem cartItem=cartItemRepository.findByIdAndCart_Id(itemId,cart.getId())
                .orElseThrow(()->new ResourceNotFoundException("Item with id "+itemId+" not found"));
        cart.getItems().remove(cartItem);
        cartItem.setCart(null);
        return cartMapper.toCartResponse(cart);
    }
}
