package com.marketplace.store.service;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.store.dto.request.CreateStoreRequest;
import com.marketplace.store.dto.request.UpdateStoreRequest;
import com.marketplace.store.dto.response.StoreResponse;
import com.marketplace.store.entity.Store;
import com.marketplace.store.mapper.StoreMapper;
import com.marketplace.store.repository.StoreRepository;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService{
    private final StoreRepository storeRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final StoreMapper storeMapper;

    @Transactional(readOnly = true)
    public Page<StoreResponse> listPublicStores(Pageable pageable) {
        return storeRepository.findAllByActiveTrueAndApprovedTrueAndSeller_Status(SellerStatus.APPROVED, pageable)
                .map(storeMapper::toResponse);
    }

    @Transactional
    public StoreResponse createStore(String username, CreateStoreRequest request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus()!= SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage a store");
        }
        if (storeRepository.existsBySeller_Id(seller.getId())){
            throw new ConflictException("This seller already has a store");
        }
        if (storeRepository.existsBySlug(request.slug())){
            throw new ConflictException("A store with slug '" + request.slug() + "' already exists");
        }
        Store store=new Store();
        store.setSeller(seller);
        store.setName(request.name());
        store.setSlug(request.slug());
        store.setDescription(request.description());
        store.setActive(true);
        store.setApproved(false);
        Store store1= storeRepository.save(store);
        return storeMapper.toResponse(store1);

    }

    @Transactional(readOnly = true)
    public StoreResponse getStoreBySlug(String slug){
        Store store=storeRepository.findBySlug(slug)
                .orElseThrow(()-> new ResourceNotFoundException("Store with slug '" + slug + "' was not found"));
        if (!store.isActive() || !store.isApproved()){
            throw new ResourceNotFoundException("Store with slug '" + slug + "' is inactive or unavailable");
        }
        return storeMapper.toResponse(store);
    }

    @Transactional(readOnly = true)
    public StoreResponse getMyStore(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        return storeMapper.toResponse(store);
    }

    @Transactional
    public StoreResponse updateMyStore(String username, UpdateStoreRequest request){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        if (seller.getStatus()!= SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage a store");
        }
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (!request.slug().equals(store.getSlug())){
            boolean existsBySlug=storeRepository.existsBySlug(request.slug());
            if (existsBySlug){
                throw new ConflictException("A store with slug '" + request.slug() + "' already exists");
            }
        }
        store.setName(request.name());
        store.setSlug(request.slug());
        store.setDescription(request.description());
        Store savedStore=storeRepository.save(store);
        return storeMapper.toResponse(savedStore);
    }

    @Transactional
    public StoreResponse deactivateMyStore(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (seller.getStatus()!= SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage a store");
        }
        if (!store.isActive()){
            return storeMapper.toResponse(store);
        }
        store.setActive(false);
        Store savedStore= storeRepository.save(store);
        return storeMapper.toResponse(savedStore);
    }

    @Transactional
    public StoreResponse activateMyStore(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        Store store=storeRepository.findBySeller_Id(seller.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Store for seller with ID '" + seller.getId() + "' was not found"));
        if (seller.getStatus()!= SellerStatus.APPROVED){
            throw new BusinessRuleException("Your seller account must be approved before you can manage a store");
        }
        if (!store.isApproved()) {
            throw new BusinessRuleException("Store requires administrator approval");
        }
        if (store.isActive()){
            return storeMapper.toResponse(store);
        }
        store.setActive(true);
        Store savedStore= storeRepository.save(store);
        return storeMapper.toResponse(savedStore);
    }
}
