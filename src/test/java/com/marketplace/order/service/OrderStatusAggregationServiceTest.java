package com.marketplace.order.service;

import com.marketplace.delivery.entity.ShipmentStatus;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusAggregationServiceTest {
    private SellerOrder part(SellerOrderStatus status) {
        SellerOrder part = new SellerOrder();
        ReflectionTestUtils.setField(part, "id", UUID.randomUUID());
        part.setStatus(status);
        return part;
    }

    @Test
    void missingShipmentCannotCompleteOrder() {
        SellerOrder a = part(SellerOrderStatus.CONFIRMED), b = part(SellerOrderStatus.CONFIRMED);
        assertThat(OrderStatusAggregationService.calculate(List.of(a, b), Map.of(a.getId(), ShipmentStatus.DELIVERED)))
                .isEqualTo(OrderStatus.PARTIALLY_DELIVERED);
    }

    @Test
    void cancellationDoesNotPreventCompletionOfDeliveredRemainder() {
        SellerOrder a = part(SellerOrderStatus.CONFIRMED), b = part(SellerOrderStatus.CANCELLED);
        assertThat(OrderStatusAggregationService.calculate(List.of(a, b), Map.of(a.getId(), ShipmentStatus.DELIVERED)))
                .isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void failureTakesPriorityOverOtherDeliveredParts() {
        SellerOrder a = part(SellerOrderStatus.CONFIRMED), b = part(SellerOrderStatus.CONFIRMED);
        assertThat(OrderStatusAggregationService.calculate(List.of(a, b),
                Map.of(a.getId(), ShipmentStatus.DELIVERED, b.getId(), ShipmentStatus.DELIVERY_FAILED)))
                .isEqualTo(OrderStatus.DELIVERY_FAILED);
    }

    @Test
    void allCancelledNeverBecomeDeliveryFailed() {
        SellerOrder a = part(SellerOrderStatus.CANCELLED);
        assertThat(OrderStatusAggregationService.calculate(List.of(a), Map.of(a.getId(), ShipmentStatus.DELIVERY_FAILED)))
                .isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void confirmationAndPartialCancellationRemainSupported() {
        SellerOrder a = part(SellerOrderStatus.CONFIRMED), b = part(SellerOrderStatus.PENDING);
        assertThat(OrderStatusAggregationService.calculate(List.of(a, b), Map.of())).isEqualTo(OrderStatus.PROCESSING);
        b.setStatus(SellerOrderStatus.CANCELLED);
        assertThat(OrderStatusAggregationService.calculate(List.of(a, b), Map.of())).isEqualTo(OrderStatus.PARTIALLY_CANCELLED);
    }
}
