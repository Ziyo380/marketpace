package com.marketplace.seller.controller;

import com.marketplace.seller.dto.request.RejectSellerRequest;
import com.marketplace.seller.dto.response.SellerResponse;
import com.marketplace.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;

    @PostMapping("/apply")
    public ResponseEntity<SellerResponse> applyForSeller(Authentication authentication){
        SellerResponse response=sellerService.applyForSeller(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<SellerResponse> getMySellerProfile(Authentication authentication){
        SellerResponse response=sellerService.getMySellerProfile(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SellerResponse>> getPendingApplications(){
        List<SellerResponse> response=sellerService.getPendingApplications();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<SellerResponse> approveSeller(@PathVariable UUID id, Authentication authentication){
        SellerResponse response=sellerService.approveSeller(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<SellerResponse> rejectSeller(@PathVariable UUID id, @Valid @RequestBody RejectSellerRequest request, Authentication authentication){
        SellerResponse response=sellerService.rejectSeller(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
