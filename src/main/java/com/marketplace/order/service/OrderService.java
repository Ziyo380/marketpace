package com.marketplace.order.service;

import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.order.dto.response.OrderResponse;
import com.marketplace.order.dto.response.OrderHistoryResponse;
import com.marketplace.order.entity.Order;
import com.marketplace.order.mapper.OrderMapper;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.repository.OrderStatusHistoryRepository;
import java.util.List;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;


    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String username, UUID orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findByIdAndBuyer_Id(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findAllByBuyer_Id(user.getId(), pageable)
                .map(orderMapper::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getMyOrderHistory(String username, UUID orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        orderRepository.findByIdAndBuyer_Id(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return historyRepository.findAllByOrder_IdOrderByCreatedAtAsc(orderId).stream()
                .map(item -> new OrderHistoryResponse(item.getId(),
                        item.getSellerOrder() == null ? null : item.getSellerOrder().getId(),
                        item.getPreviousStatus(), item.getNewStatus(), item.getActor(),
                        item.getReason(), item.getCreatedAt()))
                .toList();
    }
}
