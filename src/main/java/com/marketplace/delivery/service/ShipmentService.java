package com.marketplace.delivery.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.delivery.entity.Shipment;
import com.marketplace.delivery.entity.ShipmentActorType;
import com.marketplace.delivery.entity.ShipmentStatus;
import com.marketplace.delivery.repository.ShipmentRepository;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.SellerOrder;
import com.marketplace.order.entity.SellerOrderStatus;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.repository.SellerOrderRepository;
import com.marketplace.order.service.OrderStatusAggregationService;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final SellerOrderRepository sellerOrderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShipmentHistoryService shipmentHistoryService;
    private final OrderStatusAggregationService orderStatusAggregationService;

    @Transactional
    public Shipment createShipment(String username, UUID sellerOrderId) {
        lockOrder(sellerOrderId);
        SellerOrder sellerOrder=sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(()-> new ResourceNotFoundException("SellerOrder not found"));
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if (!sellerOrder.getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        Optional<Shipment> existingShipment= shipmentRepository.findBySellerOrder_Id(sellerOrderId);
        if (existingShipment.isPresent()) {
            return existingShipment.get();
        }
        if (sellerOrder.getStatus() != SellerOrderStatus.CONFIRMED) {
            throw new BusinessRuleException("SellerOrder must be confirmed before creating shipment");
        }
        Shipment shipment=new Shipment();
        shipment.setSellerOrder(sellerOrder);
        shipment.setStatus(ShipmentStatus.CREATED);
        return saveWithHistory(shipment, null, username, null);
    }

    @Transactional(readOnly = true)
    public Shipment getSellerShipment(String username, UUID sellerOrderId) {
        Shipment shipment = shipmentRepository.findBySellerOrder_Id(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return shipment;
    }

    @Transactional(readOnly = true)
    public List<Shipment> getCustomerShipments(String username, UUID orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        return shipmentRepository.findAllBySellerOrder_Order_Id(orderId);
    }

    @Transactional
    public Shipment updatePreparationStatus(String username, UUID sellerOrderId, ShipmentStatus targetStatus) {
        Shipment shipment = loadForUpdate(sellerOrderId);
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (targetStatus != ShipmentStatus.PACKING && targetStatus != ShipmentStatus.READY_FOR_DISPATCH) {
            throw new BusinessRuleException("Target status must be PACKING or READY_FOR_DISPATCH");
        }
        ShipmentStatus currentStatus=shipment.getStatus();
        if (currentStatus == targetStatus) {
            return shipment;
        }

        boolean allowed=(currentStatus == ShipmentStatus.CREATED && targetStatus == ShipmentStatus.PACKING)
                || (currentStatus == ShipmentStatus.PACKING && targetStatus == ShipmentStatus.READY_FOR_DISPATCH);
        if (!allowed) {
            throw new BusinessRuleException("Invalid shipment status transition");
        }
        shipment.setStatus(targetStatus);
        return saveWithHistory(shipment, currentStatus, username, null);

    }

    @Transactional
    public Shipment dispatchShipment(String username, UUID sellerOrderId, String carrierName, String trackingNumber) {
        Shipment shipment = loadForUpdate(sellerOrderId);
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (shipment.getSellerOrder().getStatus() != SellerOrderStatus.CONFIRMED) {
            throw new BusinessRuleException("SellerOrder must be confirmed before dispatch");
        }
        String normalizedCarrier=null;
        if (carrierName != null){
            normalizedCarrier=carrierName.trim();

            if (normalizedCarrier.isEmpty()){
                normalizedCarrier=null;
            }
        }
        if (normalizedCarrier != null && normalizedCarrier.length()>100){
            throw new BusinessRuleException("Carrier name cannot be longer than 100 characters");
        }

        String normalizedTracking=null;
        if (trackingNumber != null){
            normalizedTracking=trackingNumber.trim();

            if (normalizedTracking.isEmpty()){
                normalizedTracking=null;
            }
        }
        if (normalizedTracking != null && normalizedTracking.length()>128){
            throw new BusinessRuleException("Tracking number cannot be longer than 128 characters");
        }

        if (normalizedTracking != null && normalizedCarrier == null){
            throw new BusinessRuleException("Carrier name is required when tracking number is provided");
        }

        ShipmentStatus currentStatus=shipment.getStatus();
        if (currentStatus == ShipmentStatus.IN_TRANSIT) {
            if (Objects.equals(shipment.getCarrierName(), normalizedCarrier)
                    && Objects.equals(shipment.getTrackingNumber(), normalizedTracking)) {
                return shipment;
            }

            throw new ConflictException("Carrier name or tracking number is inconsistent");
        }
        if (currentStatus != ShipmentStatus.READY_FOR_DISPATCH) {
            throw new BusinessRuleException("Shipment status must be READY_FOR_DISPATCH");
        }
        shipment.setCarrierName(normalizedCarrier);
        shipment.setTrackingNumber(normalizedTracking);
        shipment.setShippedAt(Instant.now());
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        return saveWithHistory(shipment, currentStatus, username, null);
    }

    @Transactional
    public Shipment confirmDelivery(String username, UUID sellerOrderId) {
        Shipment shipment = loadForUpdate(sellerOrderId);
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (shipment.getSellerOrder().getStatus() != SellerOrderStatus.CONFIRMED) {
            throw new BusinessRuleException("SellerOrder must be confirmed before confirming shipment");
        }
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            return shipment;
        }
        if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
            throw new BusinessRuleException("Shipment status must be in TRANSIT");
        }
        shipment.setDeliveredAt(Instant.now());
        shipment.setStatus(ShipmentStatus.DELIVERED);
        return saveWithHistory(shipment, ShipmentStatus.IN_TRANSIT, username, null);
    }

    @Transactional
    public Shipment markDeliveryFailed(String username, UUID sellerOrderId, String failureReason) {
        Shipment shipment = loadForUpdate(sellerOrderId);
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        if (!shipment.getSellerOrder().getStore().getSeller().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
        if (shipment.getSellerOrder().getStatus() != SellerOrderStatus.CONFIRMED) {
            throw new BusinessRuleException("SellerOrder must be confirmed before confirming shipment");
        }
        if (failureReason == null || failureReason.isBlank()) {
            throw new BusinessRuleException("Failure reason is required");
        }
        String normalizedFailureReason=failureReason.trim();
        if (normalizedFailureReason.length() > 255){
            throw new BusinessRuleException("Failure reason cannot exceed 255 characters");
        }
        ShipmentStatus currentStatus=shipment.getStatus();
        if (currentStatus == ShipmentStatus.DELIVERY_FAILED){
            if (Objects.equals(shipment.getFailureReason(), normalizedFailureReason)){
                return shipment;
            }
            throw new ConflictException("Shipment already failed with a different reason");
        }
        if (currentStatus != ShipmentStatus.IN_TRANSIT) {
            throw new BusinessRuleException("Shipment status must be in TRANSIT");
        }
        shipment.setFailureReason(normalizedFailureReason);
        shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);
        return saveWithHistory(shipment, currentStatus, username, normalizedFailureReason);

    }

    private Shipment saveWithHistory(Shipment shipment, ShipmentStatus previousStatus,
                                     String username, String details) {
        Shipment saved = shipmentRepository.save(shipment);
        shipmentHistoryService.record(saved, previousStatus, saved.getStatus(),
                ShipmentActorType.SELLER, username, details);
        orderStatusAggregationService.recalculate(saved.getSellerOrder().getOrder(), "SELLER",
                "Shipment status changed to " + saved.getStatus().name());
        return saved;
    }

    private void lockOrder(UUID sellerOrderId) {
        UUID orderId = sellerOrderRepository.findOrderId(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller order not found"));
        orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private Shipment loadForUpdate(UUID sellerOrderId) {
        lockOrder(sellerOrderId);
        return shipmentRepository.findBySellerOrder_Id(sellerOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void createSystemShipment(SellerOrder part) {
        if (shipmentRepository.existsBySellerOrder_Id(part.getId())) return;
        Shipment shipment = new Shipment();
        shipment.setSellerOrder(part);
        shipment.setStatus(ShipmentStatus.CREATED);
        Shipment saved = shipmentRepository.save(shipment);
        shipmentHistoryService.record(saved, null, ShipmentStatus.CREATED, ShipmentActorType.SYSTEM, null, null);
    }
}
