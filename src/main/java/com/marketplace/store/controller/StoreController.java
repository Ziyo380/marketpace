package com.marketplace.store.controller;

import com.marketplace.store.dto.request.CreateStoreRequest;
import com.marketplace.store.dto.request.UpdateStoreRequest;
import com.marketplace.store.dto.response.StoreResponse;
import com.marketplace.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {
    private final StoreService storeService;

    @GetMapping
    public Page<StoreResponse> listPublicStores(Pageable pageable) {
        return storeService.listPublicStores(pageable);
    }

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(@Valid @RequestBody CreateStoreRequest request, Authentication authentication){
        StoreResponse response=storeService.createStore(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<StoreResponse> getStoreBySlug(@PathVariable String slug){
        StoreResponse response=storeService.getStoreBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<StoreResponse> getMyStore(Authentication authentication){
        StoreResponse response=storeService.getMyStore(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<StoreResponse> updateMyStore(@Valid @RequestBody UpdateStoreRequest request, Authentication authentication){
        StoreResponse response=storeService.updateMyStore(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/deactivate")
    public ResponseEntity<StoreResponse> deactivateMyStore(Authentication authentication){
        StoreResponse response=storeService.deactivateMyStore(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/activate")
    public ResponseEntity<StoreResponse> activateMyStore(Authentication authentication){
        StoreResponse response=storeService.activateMyStore(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
