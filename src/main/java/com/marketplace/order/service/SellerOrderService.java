package com.marketplace.order.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.service.StockReservationService;
import com.marketplace.order.dto.response.SellerOrderResponse;
import com.marketplace.order.dto.response.OrderHistoryResponse;
import com.marketplace.order.entity.*;
import com.marketplace.order.mapper.SellerOrderMapper;
import com.marketplace.order.repository.SellerOrderRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.repository.OrderStatusHistoryRepository;
import com.marketplace.promotion.service.PromocodeService;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerOrderService {
    private final SellerOrderRepository sellerOrderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StockReservationService stockReservationService;
    private final SellerOrderMapper sellerOrderMapper;
    private final OrderHistoryService orderHistory;
    private final OrderStatusHistoryRepository historyRepository;
    private final PromocodeService promocodeService;

    @Transactional
    public SellerOrderResponse confirmSellerOrder(String username, UUID sellerOrderId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found!"));
        SellerOrder sellerOrder=loadForUpdate(sellerOrderId);
        if (!sellerOrder.getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("SellerOrder with id: "+sellerOrderId+" not found!");
        }
        if (sellerOrder.getStatus() != SellerOrderStatus.PENDING) {
            throw new ConflictException("Seller order is not PENDING");
        } else if (sellerOrder.getItems()==null || sellerOrder.getItems().isEmpty()) {
            throw new BusinessRuleException("Seller order has no items");
        }
        rejectExpired(sellerOrder.getOrder());
        for (OrderItem orderItem : sellerOrder.getItems()){
            stockReservationService.confirmReservation(orderItem.getId());
        }
        orderHistory.record(sellerOrder.getOrder(), sellerOrder, sellerOrder.getStatus().name(),
                SellerOrderStatus.CONFIRMED.name(), "SELLER", "Seller confirmed order");
        sellerOrder.setStatus(SellerOrderStatus.CONFIRMED);
        sellerOrder.setConfirmedAt(Instant.now());
        OrderStatus previousOrderStatus = sellerOrder.getOrder().getStatus();
        updateOrderStatus(sellerOrder.getOrder());
        orderHistory.record(sellerOrder.getOrder(), null, previousOrderStatus.name(),
                sellerOrder.getOrder().getStatus().name(), "SELLER", "Seller confirmed order");
        SellerOrder savedOrder=sellerOrderRepository.saveAndFlush(sellerOrder);
        return sellerOrderMapper.toSellerOrderResponse(savedOrder);
    }

    @Transactional
    public SellerOrderResponse cancelSellerOrder(String username, UUID sellerOrderId, String reason){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found!"));
        SellerOrder sellerOrder=loadForUpdate(sellerOrderId);
        if (!sellerOrder.getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Seller order not found!");
        }
        if (sellerOrder.getStatus() != SellerOrderStatus.PENDING) {
            throw new ConflictException("Only PENDING seller orders can be cancelled");
        }
        rejectExpired(sellerOrder.getOrder());
        if (reason==null || reason.isBlank()){
            throw new BusinessRuleException("Cancellation reason is required");
        }
        if (sellerOrder.getItems()==null || sellerOrder.getItems().isEmpty()) {
            throw new BusinessRuleException("Seller order has no items");
        }
        for (OrderItem orderItem : sellerOrder.getItems()){
            stockReservationService.releaseReservation(orderItem.getId());
        }
        orderHistory.record(sellerOrder.getOrder(), sellerOrder, sellerOrder.getStatus().name(),
                SellerOrderStatus.CANCELLED.name(), "SELLER", reason.trim());
        sellerOrder.setStatus(SellerOrderStatus.CANCELLED);
        sellerOrder.setCancelledAt(Instant.now());
        sellerOrder.setCancellationReason(reason.trim());
        OrderStatus previousOrderStatus = sellerOrder.getOrder().getStatus();
        updateOrderStatus(sellerOrder.getOrder());
        orderHistory.record(sellerOrder.getOrder(), null, previousOrderStatus.name(),
                sellerOrder.getOrder().getStatus().name(), "SELLER", reason.trim());
        if (sellerOrder.getOrder().getStatus() == OrderStatus.CANCELLED) {
            promocodeService.release(sellerOrder.getOrder());
        }
        SellerOrder savedOrder=sellerOrderRepository.saveAndFlush(sellerOrder);
        return sellerOrderMapper.toSellerOrderResponse(savedOrder);

    }


    @Transactional(readOnly = true)
    public Page<SellerOrderResponse> getMySellerOrders(String username, Pageable pageable){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found!"));
        Page<SellerOrder> sellerOrder=sellerOrderRepository.findAllByStore_Seller_User_Id(user.getId(),pageable);
        return sellerOrder.map(sellerOrderMapper::toSellerOrderResponse);

    }

    @Transactional(readOnly = true)
    public SellerOrderResponse getMySellerOrder(String username, UUID sellerOrderId){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found!"));
        SellerOrder sellerOrder=sellerOrderRepository.findByIdAndStore_Seller_User_Id(sellerOrderId,user.getId())
                .orElseThrow(()-> new  ResourceNotFoundException("SellerOrder with id: "+sellerOrderId+" not found!"));
        return sellerOrderMapper.toSellerOrderResponse(sellerOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> getMySellerOrderHistory(String username, UUID sellerOrderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        sellerOrderRepository.findByIdAndStore_Seller_User_Id(sellerOrderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller order not found"));
        return historyRepository.findAllBySellerOrder_IdOrderByCreatedAtAsc(sellerOrderId).stream()
                .map(item -> new OrderHistoryResponse(item.getId(), sellerOrderId,
                        item.getPreviousStatus(), item.getNewStatus(), item.getActor(),
                        item.getReason(), item.getCreatedAt()))
                .toList();
    }




    private void updateOrderStatus(Order order){
        List<SellerOrder> sellerOrders=order.getSellerOrders();

        if (sellerOrders.isEmpty()){
            throw new BusinessRuleException("Order has no seller orders");
        }

        boolean allCancelled=sellerOrders.stream()
                .allMatch(sellerOrder->sellerOrder.getStatus()==SellerOrderStatus.CANCELLED);
        boolean anyCancelled=sellerOrders.stream()
                .anyMatch(sellerOrder->sellerOrder.getStatus()==SellerOrderStatus.CANCELLED);
        boolean allConfirmed=sellerOrders.stream()
                .allMatch(sellerOrder->sellerOrder.getStatus()==SellerOrderStatus.CONFIRMED);
        boolean anyConfirmed=sellerOrders.stream()
                .anyMatch(sellerOrder->sellerOrder.getStatus()==SellerOrderStatus.CONFIRMED);

        if (allCancelled){
            order.setStatus(OrderStatus.CANCELLED);
        } else if (anyCancelled) {
            order.setStatus(OrderStatus.PARTIALLY_CANCELLED);

        } else if (allConfirmed) {
            order.setStatus(OrderStatus.CONFIRMED);
        }  else if (anyConfirmed) {
            order.setStatus(OrderStatus.PROCESSING);
        } else  {
            order.setStatus(OrderStatus.PENDING);
        }
    }

    private SellerOrder loadForUpdate(UUID sellerOrderId) {
        UUID orderId = sellerOrderRepository.findOrderId(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller order not found"));
        orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller order not found"));
    }

    private void rejectExpired(Order order) {
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("Order reservation has expired");
        }
    }
}
