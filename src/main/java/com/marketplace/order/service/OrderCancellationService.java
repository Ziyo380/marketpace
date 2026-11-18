package com.marketplace.order.service;

import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.service.StockReservationService;
import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.order.mapper.OrderMapper;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.promotion.service.PromocodeService;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.util.UUID;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final StockReservationService stockReservationService;
    private final OrderHistoryService orderHistory;
    private final PromocodeService promocodeService;

    @Transactional
    public OrderResponse cancelMyOrder(String username, UUID orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order status must be PENDING");
        }

        cancelPendingSellerOrders(order, "Buyer cancelled order", "CUSTOMER");
        orderHistory.record(order, null, order.getStatus().name(), OrderStatus.CANCELLED.name(),
                "CUSTOMER", "Buyer cancelled order");
        order.setStatus(OrderStatus.CANCELLED);
        promocodeService.release(order);
        Order savedOrder = orderRepository.saveAndFlush(order);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public void expireOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getExpiresAt() == null || order.getExpiresAt().isAfter(Instant.now())
                || (order.getStatus() != OrderStatus.PENDING
                    && order.getStatus() != OrderStatus.PROCESSING
                    && order.getStatus() != OrderStatus.PARTIALLY_CANCELLED)) {
            return;
        }
        cancelPendingSellerOrders(order, "Reservation expired", "SYSTEM");
        boolean anyConfirmed = order.getSellerOrders().stream()
                .anyMatch(part -> part.getStatus() == SellerOrderStatus.CONFIRMED);
        OrderStatus next = anyConfirmed ? OrderStatus.PARTIALLY_CANCELLED : OrderStatus.CANCELLED;
        orderHistory.record(order, null, order.getStatus().name(), next.name(),
                "SYSTEM", "Reservation expired");
        order.setStatus(next);
        if (next == OrderStatus.CANCELLED) promocodeService.release(order);
        orderRepository.saveAndFlush(order);
    }

    private void cancelPendingSellerOrders(Order order, String reason, String actor) {
        Instant now = Instant.now();
        for (SellerOrder sellerOrder : order.getSellerOrders()) {
            if (sellerOrder.getStatus() != SellerOrderStatus.PENDING) {
                continue;
            }
            for (var item : sellerOrder.getItems()) {
                stockReservationService.releaseReservation(item.getId());
            }
            orderHistory.record(order, sellerOrder, sellerOrder.getStatus().name(),
                    SellerOrderStatus.CANCELLED.name(), actor, reason);
            sellerOrder.setStatus(SellerOrderStatus.CANCELLED);
            sellerOrder.setCancelledAt(now);
            sellerOrder.setCancellationReason(reason);
        }
    }
}
