package com.marketplace.order.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.delivery.entity.Shipment;
import com.marketplace.delivery.entity.ShipmentStatus;
import com.marketplace.delivery.repository.ShipmentRepository;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusAggregationService {
    private final ShipmentRepository shipmentRepository;
    private final OrderHistoryService orderHistory;

    // The caller must lock the Order before changing any of its parts or shipments.
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(Order order, String actor, String reason) {
        List<SellerOrder> parts = order.getSellerOrders();
        if (parts.isEmpty()) throw new BusinessRuleException("Order has no seller orders");
        Map<UUID, ShipmentStatus> shipments = shipmentRepository.findAllBySellerOrder_Order_Id(order.getId())
                .stream().collect(Collectors.toMap(s -> s.getSellerOrder().getId(), Shipment::getStatus));
        OrderStatus next = calculate(parts, shipments);
        if (order.getStatus() != next) {
            orderHistory.record(order, null, order.getStatus().name(), next.name(), actor, reason);
            order.setStatus(next);
        }
    }

    static OrderStatus calculate(List<SellerOrder> parts, Map<UUID, ShipmentStatus> shipments) {
        List<SellerOrder> active = parts.stream()
                .filter(p -> p.getStatus() != SellerOrderStatus.CANCELLED).toList();
        if (active.isEmpty()) return OrderStatus.CANCELLED;
        boolean anyFailed = active.stream().anyMatch(p -> shipments.get(p.getId()) == ShipmentStatus.DELIVERY_FAILED);
        boolean allDelivered = active.stream().allMatch(p -> p.getStatus() == SellerOrderStatus.CONFIRMED
                && shipments.get(p.getId()) == ShipmentStatus.DELIVERED);
        boolean anyDelivered = active.stream().anyMatch(p -> shipments.get(p.getId()) == ShipmentStatus.DELIVERED);
        boolean anyInTransit = active.stream().anyMatch(p -> shipments.get(p.getId()) == ShipmentStatus.IN_TRANSIT);
        if (anyFailed) return OrderStatus.DELIVERY_FAILED;
        if (allDelivered) return OrderStatus.DELIVERED;
        if (anyDelivered) return OrderStatus.PARTIALLY_DELIVERED;
        if (anyInTransit) return OrderStatus.IN_DELIVERY;
        if (active.size() != parts.size()) return OrderStatus.PARTIALLY_CANCELLED;
        boolean anyPending = active.stream().anyMatch(p -> p.getStatus() == SellerOrderStatus.PENDING);
        boolean anyConfirmed = active.stream().anyMatch(p -> p.getStatus() == SellerOrderStatus.CONFIRMED);
        if (anyPending) return anyConfirmed ? OrderStatus.PROCESSING : OrderStatus.PENDING;
        return OrderStatus.CONFIRMED;
    }
}
