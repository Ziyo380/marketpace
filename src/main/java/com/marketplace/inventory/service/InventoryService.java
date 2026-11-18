package com.marketplace.inventory.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.inventory.dto.response.InventoryResponse;
import com.marketplace.inventory.dto.response.InventoryMovementResponse;
import com.marketplace.inventory.entity.Inventory;
import com.marketplace.inventory.entity.InventoryMovementType;
import com.marketplace.inventory.mapper.InventoryMapper;
import com.marketplace.inventory.repository.InventoryRepository;
import com.marketplace.inventory.repository.InventoryMovementRepository;
import com.marketplace.product.entity.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.entity.Store;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper  inventoryMapper;
    private final InventoryMovementService movementService;
    private final InventoryMovementRepository movementRepository;


    @Transactional
    public InventoryResponse addStock(String username, UUID productId, int amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with this username not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller with this id " + user.getId() + " not found"));
        if (!seller.getStatus().equals(SellerStatus.APPROVED)){
            throw new BusinessRuleException("Seller is not approved");
        }
        Store store = storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store with this id " + seller.getId() + " not found"));
        if (!store.isActive()){
            throw new BusinessRuleException("Store is not active");
        }
        Product product = productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with this id " + productId + " not found"));
        Inventory inventory = inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        int newQuantity;
        try {
            newQuantity = Math.addExact(inventory.getQuantity(), amount);
            inventory.setQuantity(newQuantity);
        }catch (ArithmeticException e){
            throw new BusinessRuleException("Quantity exceeds the maximum allowed value");
        }
        inventoryRepository.save(inventory);
        movementService.record(inventory, InventoryMovementType.STOCK_ADDED, amount, 0);
        InventoryResponse response = inventoryMapper.toResponse(inventory);
        return response;
    }


    @Transactional
    public InventoryResponse removeStock(String username, UUID productId, int amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with this username not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller with this id " + user.getId() + " not found"));
        if (!seller.getStatus().equals(SellerStatus.APPROVED)){
            throw new BusinessRuleException("Seller is not approved");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store with this id " + seller.getId() + " not found"));
        if (!store.isActive()){
            throw new BusinessRuleException("Store is not active");
        }
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with this id " + productId + " not found"));
        Inventory inventory=inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        int available= inventory.getQuantity()-inventory.getReservedQuantity();
        if (amount>available){
            throw new BusinessRuleException("Amount exceeds the maximum allowed value");
        }
        int newQuantity= inventory.getQuantity()-amount;
        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);
        movementService.record(inventory, InventoryMovementType.STOCK_REMOVED, -amount, 0);
        InventoryResponse response=inventoryMapper.toResponse(inventory);
        return response;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(String username,UUID productId) {
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with this username not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller with this id " + user.getId() + " not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store with this id " + seller.getId() + " not found"));
        Product product=productRepository.findByIdAndStore_Id(productId,store.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Product with this id " + productId + " not found"));
        Inventory inventory=inventoryRepository.findByProduct_Id(product.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        InventoryResponse response = inventoryMapper.toResponse(inventory);
        return response;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> getMovements(String username, UUID productId) {
        getInventoryByProductId(username, productId);
        Inventory inventory = inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        return movementRepository.findAllByInventory_IdOrderByCreatedAtAsc(inventory.getId()).stream()
                .map(item -> new InventoryMovementResponse(item.getId(), item.getType(),
                        item.getQuantityDelta(), item.getReservedDelta(),
                        item.getQuantityAfter(), item.getReservedAfter(), item.getCreatedAt()))
                .toList();
    }

    @Transactional
    public InventoryResponse reserveStock(UUID productId, int amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        Inventory inventory=inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        int available= inventory.getQuantity()-inventory.getReservedQuantity();
        if (amount>available){
            throw new BusinessRuleException("Amount exceeds the maximum allowed value");
        }
        int newReserved= inventory.getReservedQuantity()+amount;
        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        movementService.record(inventory, InventoryMovementType.RESERVED, 0, amount);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse releaseStock(UUID productId, int amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        Inventory inventory=inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        if (amount>inventory.getReservedQuantity()){
            throw new BusinessRuleException("Amount exceeds the maximum allowed value");
        }
        int newReserved= inventory.getReservedQuantity()-amount;
        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        movementService.record(inventory, InventoryMovementType.RELEASED, 0, -amount);
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse confirmStock(UUID productId, int amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than zero");
        }
        Inventory inventory=inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Inventory with this id " + productId + " not found"));
        if (amount>inventory.getReservedQuantity()){
            throw new BusinessRuleException("Amount exceeds the maximum allowed value");
        }
        int newQuantity= inventory.getQuantity()-amount;
        int newReserved= inventory.getReservedQuantity()-amount;
        inventory.setQuantity(newQuantity);
        inventory.setReservedQuantity(newReserved);
        inventoryRepository.save(inventory);
        movementService.record(inventory, InventoryMovementType.CONFIRMED, -amount, -amount);
        return inventoryMapper.toResponse(inventory);
    }
}
