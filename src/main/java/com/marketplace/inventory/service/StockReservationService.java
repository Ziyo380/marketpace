package com.marketplace.inventory.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.entity.StockReservation;
import com.marketplace.inventory.entity.StockReservationStatus;
import com.marketplace.inventory.repository.InventoryRepository;
import com.marketplace.inventory.repository.StockReservationRepository;
import com.marketplace.order.entity.OrderItem;
import com.marketplace.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockReservationService {
    private final StockReservationRepository stockReservationRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final ProductVariantService variantService;

    @Transactional
    public StockReservation createReservation(OrderItem  orderItem) {
        if (orderItem==null) {
            throw new BusinessRuleException("Order Item is null");
        } else if (orderItem.getId()==null) {
            throw new BusinessRuleException("Order Item id is null");
            
        } else if (orderItem.getProduct()==null) {
            throw new BusinessRuleException("Product is null");
        } else if (orderItem.getProduct().getId()==null) {
            throw new BusinessRuleException("Product id is null");
        }else if (orderItem.getQuantity()<=0){
            throw new BusinessRuleException("Quantity is empty");
        }
        Optional<StockReservation> stockReservation = stockReservationRepository.findByOrderItem_Id(orderItem.getId());
        if (stockReservation.isPresent()) {
            throw new BusinessRuleException("Stock Reservation already exists");
        }
        StockReservation reservation=new StockReservation();
        if (orderItem.getVariant() != null) {
            variantService.reserve(orderItem.getVariant().getId(), orderItem.getQuantity());
            reservation.setVariant(orderItem.getVariant());
        } else {
            Inventory inventory=inventoryRepository.findByProduct_Id(orderItem.getProduct().getId())
                    .orElseThrow(()->new ResourceNotFoundException("Inventory with id "+orderItem.getProduct().getId()+" not found"));
            inventoryService.reserveStock(orderItem.getProduct().getId(), orderItem.getQuantity());
            reservation.setInventory(inventory);
        }
        reservation.setOrderItem(orderItem);
        reservation.setQuantity(orderItem.getQuantity());
        reservation.setStatus(StockReservationStatus.ACTIVE);
        reservation.setClosedAt(null);
        StockReservation savedReservation=stockReservationRepository.save(reservation);
        return savedReservation;
    }

    @Transactional
    public StockReservation confirmReservation(UUID orderItemId) {
        StockReservation reservation=stockReservationRepository.findByOrderItem_Id(orderItemId)
                .orElseThrow(()->new ResourceNotFoundException("Stock Reservation Not Found"));
        if (reservation.getStatus()!=StockReservationStatus.ACTIVE) {
            throw new BusinessRuleException("Stock Reservation Status Not Active");
        }
        if (reservation.getVariant() != null) {
            variantService.confirm(reservation.getVariant().getId(), reservation.getQuantity());
        } else {
            inventoryService.confirmStock(reservation.getInventory().getProduct().getId(), reservation.getQuantity());
        }
        reservation.setStatus(StockReservationStatus.CONFIRMED);
        reservation.setClosedAt(Instant.now());
        StockReservation savedReservation=stockReservationRepository.saveAndFlush(reservation);
        return savedReservation;
    }

    @Transactional
    public StockReservation releaseReservation(UUID orderItemId) {
        StockReservation reservation=stockReservationRepository.findByOrderItem_Id(orderItemId)
                .orElseThrow(()->new ResourceNotFoundException("Stock Reservation Not Found"));
        if (reservation.getStatus()!=StockReservationStatus.ACTIVE) {
            throw new BusinessRuleException("Stock Reservation Status Not Active");
        }
        if (reservation.getVariant() != null) {
            variantService.release(reservation.getVariant().getId(), reservation.getQuantity());
        } else {
            inventoryService.releaseStock(reservation.getInventory().getProduct().getId(), reservation.getQuantity());
        }
        reservation.setStatus(StockReservationStatus.RELEASED);
        reservation.setClosedAt(Instant.now());
        StockReservation savedReservation=stockReservationRepository.saveAndFlush(reservation);
        return savedReservation;
    }
}
