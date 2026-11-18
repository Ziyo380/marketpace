package com.marketplace.favorite.controller;

import com.marketplace.favorite.dto.response.FavoriteResponse;
import com.marketplace.favorite.service.FavoriteService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService service;

    @GetMapping
    public Page<FavoriteResponse> list(Authentication authentication, Pageable pageable) {
        return service.list(authentication.getName(), pageable);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FavoriteResponse> add(@PathVariable UUID productId, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(authentication.getName(), productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable UUID productId, Authentication authentication) {
        service.remove(authentication.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}
