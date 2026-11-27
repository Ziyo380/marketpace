package com.marketplace.order.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderPricingService {
    private static final BigDecimal CENT = new BigDecimal("0.01");

    /** Freezes the item, delivery and discount amounts for a new checkout. */
    public void priceCheckout(Order order) {
        List<SellerOrder> parts = new ArrayList<>(order.getSellerOrders());
        if (parts.isEmpty()) {
            throw new BusinessRuleException("Order has no seller orders");
        }
        parts.sort(Comparator.comparing(part -> part.getStore().getId()));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal delivery = BigDecimal.ZERO;
        for (SellerOrder part : parts) {
            subtotal = subtotal.add(money(part.getTotalAmount(), "Seller item subtotal"));
            delivery = delivery.add(money(part.getDeliveryFee(), "Seller delivery fee"));
        }
        BigDecimal discount = money(order.getDiscountAmount(), "Order discount");
        if (subtotal.signum() <= 0 || discount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Order discount exceeds item subtotal");
        }

        Map<SellerOrder, BigDecimal> allocations = allocateDiscount(parts, subtotal, discount);
        BigDecimal total = BigDecimal.ZERO;
        for (SellerOrder part : parts) {
            BigDecimal partDiscount = allocations.get(part);
            BigDecimal payable = part.getTotalAmount().add(part.getDeliveryFee()).subtract(partDiscount);
            if (payable.signum() < 0) {
                throw new IllegalStateException("Seller order payable amount is negative");
            }
            part.setDiscountAmount(partDiscount);
            part.setPayableAmount(payable);
            total = total.add(payable);
        }
        if (total.compareTo(subtotal.add(delivery).subtract(discount)) != 0) {
            throw new IllegalStateException("Order and seller order totals disagree");
        }
        order.setSubtotalAmount(subtotal);
        order.setDeliveryTotalAmount(delivery);
        order.setTotalAmount(total);
        recalculateCurrentTotal(order);
    }

    /** Only the current obligation changes; the checkout snapshot stays intact. */
    public void recalculateCurrentTotal(Order order) {
        if (order.getSellerOrders().isEmpty()) {
            throw new BusinessRuleException("Order has no seller orders");
        }
        BigDecimal current = BigDecimal.ZERO;
        for (SellerOrder part : order.getSellerOrders()) {
            if (part.getStatus() == SellerOrderStatus.CANCELLED) {
                continue;
            }
            if (part.getPayableAmount() == null) {
                // Historical orders have no reliable per-store discount allocation.
                order.setCurrentTotalAmount(null);
                return;
            }
            current = current.add(part.getPayableAmount());
        }
        if (order.getTotalAmount() != null && current.compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalStateException("Current order amount exceeds checkout total");
        }
        order.setCurrentTotalAmount(current);
    }

    private Map<SellerOrder, BigDecimal> allocateDiscount(
            List<SellerOrder> parts, BigDecimal subtotal, BigDecimal discount) {
        Map<SellerOrder, BigDecimal> allocations = new IdentityHashMap<>();
        List<DiscountRemainder> remainders = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO;
        for (SellerOrder part : parts) {
            BigDecimal numerator = discount.multiply(part.getTotalAmount());
            BigDecimal base = numerator.divide(subtotal, 2, RoundingMode.DOWN);
            allocations.put(part, base);
            remainders.add(new DiscountRemainder(part, numerator.subtract(base.multiply(subtotal))));
            assigned = assigned.add(base);
        }
        remainders.sort(Comparator.comparing(DiscountRemainder::remainder).reversed()
                .thenComparing(item -> item.part().getStore().getId()));
        int remainingCents = discount.subtract(assigned).movePointRight(2).intValueExact();
        if (remainingCents > remainders.size()) {
            throw new IllegalStateException("Discount allocation has an invalid remainder");
        }
        for (int i = 0; i < remainingCents; i++) {
            SellerOrder part = remainders.get(i).part();
            allocations.put(part, allocations.get(part).add(CENT));
        }
        return allocations;
    }

    private BigDecimal money(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessRuleException(field + " must be non-negative");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BusinessRuleException(field + " has more than two decimal places");
        }
    }

    private record DiscountRemainder(SellerOrder part, BigDecimal remainder) { }
}
