package com.marketplace.cart.mapper;

import com.marketplace.cart.dto.response.CartItemResponse;
import com.marketplace.cart.dto.response.CartResponse;
import com.marketplace.cart.entity.Cart;
import com.marketplace.cart.entity.CartItem;
import com.marketplace.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartMapper {
    CartItemResponse toItemResponse(CartItem cartItem) {
        Product product=cartItem.getProduct();

        BigDecimal unitPrice=cartItem.getVariant() == null ? product.getPrice() : cartItem.getVariant().getPrice();
        int quantity=cartItem.getQuantity();

        BigDecimal subtotal=unitPrice.multiply(BigDecimal.valueOf(quantity));

        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                cartItem.getVariant() == null ? null : cartItem.getVariant().getId(),
                cartItem.getVariant() == null ? null : cartItem.getVariant().getSku(),
                cartItem.getVariant() == null ? product.getName()
                        : product.getName() + " — " + cartItem.getVariant().getName(),
                unitPrice,
                quantity,
                subtotal
        );
    }

    public CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items=cart.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();
        int totalItems=items.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();
        BigDecimal totalPrice=items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                items,
                totalItems,
                totalPrice
        );
    }
}
