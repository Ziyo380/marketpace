package com.marketplace.delivery.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.delivery.dto.response.ShipmentHistoryResponse;
import com.marketplace.delivery.entity.Shipment;
import com.marketplace.delivery.entity.ShipmentActorType;
import com.marketplace.delivery.entity.ShipmentStatus;
import com.marketplace.delivery.entity.ShipmentStatusHistory;
import com.marketplace.delivery.mapper.ShipmentHistoryMapper;
import com.marketplace.delivery.repository.ShipmentRepository;
import com.marketplace.delivery.repository.ShipmentStatusHistoryRepository;
import com.marketplace.order.entity.Order;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentHistoryService {
    private final ShipmentStatusHistoryRepository historyRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ShipmentHistoryMapper mapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Shipment shipment, ShipmentStatus fromStatus, ShipmentStatus toStatus,
                       ShipmentActorType actorType, String actorUsername, String details) {
        Objects.requireNonNull(shipment, "Shipment is required");
        Objects.requireNonNull(toStatus, "Target status is required");
        Objects.requireNonNull(actorType, "Actor type is required");
        if (fromStatus == toStatus) return;
        if ((fromStatus == null) != (toStatus == ShipmentStatus.CREATED)) {
            throw new BusinessRuleException("Only a creation event may have no previous status");
        }
        if (shipment.getStatus() != toStatus) {
            throw new BusinessRuleException("History status must match shipment status");
        }
        ShipmentStatusHistory history = new ShipmentStatusHistory();
        history.setShipment(shipment);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setActorType(actorType);
        history.setActorUsername(normalize(actorUsername, 100, "Actor username"));
        history.setDetails(normalize(details, 255, "History details"));
        history.setChangedAt(Instant.now());
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<ShipmentHistoryResponse> getSellerHistory(String username, UUID sellerOrderId) {
        Shipment shipment = shipmentRepository.findBySellerOrder_Id(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        User user = currentUser(username);
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return responses(shipment.getId());
    }

    @Transactional(readOnly = true)
    public List<ShipmentHistoryResponse> getCustomerHistory(String username, UUID orderId, UUID shipmentId) {
        User user = currentUser(username);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        if (!shipment.getSellerOrder().getOrder().getId().equals(orderId)) {
            throw new ResourceNotFoundException("Shipment not found in this order");
        }
        return responses(shipmentId);
    }

    private User currentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private List<ShipmentHistoryResponse> responses(UUID shipmentId) {
        return historyRepository.findAllByShipment_IdOrderByChangedAtAsc(shipmentId)
                .stream().map(mapper::toResponse).toList();
    }

    private String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessRuleException(field + " is too long");
        }
        return normalized;
    }
}
