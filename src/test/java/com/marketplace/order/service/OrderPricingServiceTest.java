package com.marketplace.order.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.store.entity.Store;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderPricingServiceTest {
    private final OrderPricingService pricing = new OrderPricingService();

    @Test
    void withoutPromocodeChargesDeliveryOnce() {
        SellerOrder part = part(1, "100.00", "20.00");
        Order order = order("0.00", part);

        pricing.priceCheckout(order);

        assertThat(part.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(part.getPayableAmount()).isEqualByComparingTo("120.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(order.getCurrentTotalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void allocatesDiscountOnlyAcrossItemsAndKeepsOriginalTotalAfterPartialCancellation() {
        SellerOrder first = part(1, "500000.00", "20000.00");
        SellerOrder second = part(2, "300000.00", "30000.00");
        Order order = order("80000.00", first, second);

        pricing.priceCheckout(order);

        assertThat(first.getDiscountAmount()).isEqualByComparingTo("50000.00");
        assertThat(second.getDiscountAmount()).isEqualByComparingTo("30000.00");
        assertThat(first.getPayableAmount()).isEqualByComparingTo("470000.00");
        assertThat(second.getPayableAmount()).isEqualByComparingTo("300000.00");
        assertThat(order.getSubtotalAmount()).isEqualByComparingTo("800000.00");
        assertThat(order.getDeliveryTotalAmount()).isEqualByComparingTo("50000.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("770000.00");
        assertThat(order.getCurrentTotalAmount()).isEqualByComparingTo("770000.00");

        second.setStatus(SellerOrderStatus.CANCELLED);
        pricing.recalculateCurrentTotal(order);
        assertThat(order.getCurrentTotalAmount()).isEqualByComparingTo("470000.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("770000.00");

        first.setStatus(SellerOrderStatus.CANCELLED);
        pricing.recalculateCurrentTotal(order);
        assertThat(order.getCurrentTotalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void distributesRoundingCentDeterministically() {
        SellerOrder first = part(1, "0.01", "0.00");
        SellerOrder second = part(2, "0.01", "0.00");
        SellerOrder third = part(3, "0.01", "0.00");
        Order order = order("0.01", third, second, first);

        pricing.priceCheckout(order);

        assertThat(first.getDiscountAmount()).isEqualByComparingTo("0.01");
        assertThat(second.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(third.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.02");
    }

    @Test
    void leavesHistoricalCurrentAmountUnknownUntilAllPartsAreCancelled() {
        SellerOrder historical = part(1, "100.00", "0.00");
        Order order = order("0.00", historical);
        order.setTotalAmount(new BigDecimal("100.00"));

        pricing.recalculateCurrentTotal(order);
        assertThat(order.getCurrentTotalAmount()).isNull();

        historical.setStatus(SellerOrderStatus.CANCELLED);
        pricing.recalculateCurrentTotal(order);
        assertThat(order.getCurrentTotalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsDiscountGreaterThanItemSubtotal() {
        Order order = order("100.01", part(1, "100.00", "20.00"));

        assertThatThrownBy(() -> pricing.priceCheckout(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order discount exceeds item subtotal");
    }

    private Order order(String discount, SellerOrder... parts) {
        Order order = new Order();
        order.setDiscountAmount(new BigDecimal(discount));
        for (SellerOrder part : parts) {
            part.setOrder(order);
            order.getSellerOrders().add(part);
        }
        return order;
    }

    private SellerOrder part(int storeNumber, String itemSubtotal, String deliveryFee) {
        Store store = new Store();
        ReflectionTestUtils.setField(store, "id", new UUID(0, storeNumber));
        SellerOrder part = new SellerOrder();
        part.setStore(store);
        part.setStatus(SellerOrderStatus.PENDING);
        part.setTotalAmount(new BigDecimal(itemSubtotal));
        part.setDeliveryFee(new BigDecimal(deliveryFee));
        return part;
    }
}
