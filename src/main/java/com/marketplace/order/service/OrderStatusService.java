package com.marketplace.order.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.service.StockReservationService;
import com.marketplace.delivery.service.ShipmentService;
import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import java.time.Instant;
import com.marketplace.order.mapper.OrderMapper;
import com.marketplace.order.repository.OrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final StockReservationService stockReservationService;
    private final OrderHistoryService orderHistory;
    private final ShipmentService shipmentService;
    private final OrderStatusAggregationService aggregation;

    @Transactional
    public OrderResponse confirmOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order status must be PENDING");
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Order reservation has expired");
        }
        if (order.getSellerOrders().isEmpty()) {
            throw new BusinessRuleException("Order has no seller orders");
        }
        for (SellerOrder part : order.getSellerOrders()) {
            if (part.getStatus() != SellerOrderStatus.PENDING || part.getItems().isEmpty()) {
                throw new BusinessRuleException("All seller orders must be PENDING and contain items");
            }
            for (var item : part.getItems()) {
                stockReservationService.confirmReservation(item.getId());
            }
            orderHistory.record(order, part, part.getStatus().name(),
                    SellerOrderStatus.CONFIRMED.name(), "SYSTEM", "Order confirmed");
            part.setStatus(SellerOrderStatus.CONFIRMED);
            part.setConfirmedAt(Instant.now());
            shipmentService.createSystemShipment(part);
        }

        aggregation.recalculate(order, "SYSTEM", "Order confirmed");
        Order savedOrder = orderRepository.saveAndFlush(order);
        return orderMapper.toOrderResponse(savedOrder);
    }
}
