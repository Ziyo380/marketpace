package com.marketplace.seller.service;
import com.marketplace.admin.service.AuditService;

import com.marketplace.common.exception.BusinessRuleException;
import com.marketplace.common.exception.ConflictException;
import com.marketplace.common.exception.ResourceNotFoundException;
import com.marketplace.seller.dto.request.RejectSellerRequest;
import com.marketplace.seller.dto.response.SellerResponse;
import com.marketplace.seller.entity.Seller;
import com.marketplace.seller.entity.SellerStatus;
import com.marketplace.seller.mapper.SellerMapper;
import com.marketplace.seller.repository.SellerRepository;
import com.marketplace.user.entity.Role;
import com.marketplace.user.entity.RoleName;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.RoleRepository;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final AuditService auditService;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final SellerMapper sellerMapper;
    private final RoleRepository roleRepository;
    private final Clock clock;

    @Transactional
    public SellerResponse applyForSeller(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        boolean alreadyApplied= sellerRepository.existsByUser_Id(user.getId());
        if (alreadyApplied){
            throw new ConflictException("A seller application already exists for this user");
        }
            Seller seller=new Seller();
            seller.setUser(user);
            seller.setStatus(SellerStatus.PENDING);
            Seller savedSeller=sellerRepository.save(seller);
            return sellerMapper.toResponse(savedSeller);
    }

    @Transactional(readOnly = true)
    public SellerResponse getMySellerProfile(String username){
        User user=userRepository.findByUsername(username)
                .orElseThrow(()-> new ResourceNotFoundException("User with username '" + username + "' was not found"));
        Seller seller=sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(()-> new ResourceNotFoundException("Seller profile for user with ID '" + user.getId() + "' was not found"));
        return sellerMapper.toResponse(seller);
    }

    @Transactional(readOnly = true)
    public List<SellerResponse>  getPendingApplications(){
        List<Seller> pendingSellers=sellerRepository.findAllByStatus(SellerStatus.PENDING);
        List<SellerResponse> response=pendingSellers
                .stream()
                .map(sellerMapper::toResponse)
                .toList();
        return response;
    }

    @Transactional
    public SellerResponse approveSeller(UUID sellerId, String actorName){
        Seller seller=sellerRepository.findById(sellerId)
                .orElseThrow(()-> new ResourceNotFoundException("Seller application with ID '" + sellerId + "' was not found"));
        if (seller.getStatus() != SellerStatus.PENDING){
            throw new BusinessRuleException("Only pending seller applications can be approved");
        }
        Role sellerRole=roleRepository.findByName(RoleName.SELLER)
                .orElseThrow(()-> new IllegalStateException("Required role SELLER is not configured"));
        User user=seller.getUser();
        user.addRole(sellerRole);
        seller.setStatus(SellerStatus.APPROVED);
        seller.setApprovedAt(LocalDateTime.now(clock));
        seller.setRejectionReason(null);
        Seller savedSeller=sellerRepository.save(seller);
        User actor = userRepository.findByUsername(actorName)
                .orElseThrow(() -> new ResourceNotFoundException("Administrator not found"));
        auditService.record(actor, "APPROVE_SELLER", "SELLER", sellerId, null);
        return sellerMapper.toResponse(savedSeller);
    }

    @Transactional
    public SellerResponse rejectSeller(UUID sellerId, RejectSellerRequest request, String actorName){
        Seller seller=sellerRepository.findById(sellerId)
                .orElseThrow(()-> new ResourceNotFoundException("Seller application with ID '" + sellerId + "' was not found"));
        if (seller.getStatus() != SellerStatus.PENDING){
            throw new BusinessRuleException("Only pending seller applications can be rejected");
        }

        seller.setStatus(SellerStatus.REJECTED);
        seller.setApprovedAt(null);
        seller.setRejectionReason(request.rejectionReason());
        Seller savedSeller=sellerRepository.save(seller);
        User actor = userRepository.findByUsername(actorName)
                .orElseThrow(() -> new ResourceNotFoundException("Administrator not found"));
        auditService.record(actor, "REJECT_SELLER", "SELLER", sellerId, request.rejectionReason());
        return sellerMapper.toResponse(savedSeller);

    }
}
