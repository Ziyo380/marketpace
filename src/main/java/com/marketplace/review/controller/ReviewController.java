package com.marketplace.review.controller;

import com.marketplace.review.dto.request.CreateReviewRequest;
import com.marketplace.review.dto.request.UpdateReviewRequest;
import com.marketplace.review.dto.response.ProductRatingResponse;
import com.marketplace.review.dto.response.ReviewResponse;
import com.marketplace.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(authentication.getName(), request));
    }
    @PutMapping("/{id}")
    public ReviewResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {
        return service.update(authentication.getName(), id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        service.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/me")
    public Page<ReviewResponse> myReviews(Authentication authentication, Pageable pageable) {
        return service.myReviews(authentication.getName(), pageable);
    }
    @GetMapping("/products/{productId}")
    public Page<ReviewResponse> publicReviews(@PathVariable UUID productId, Pageable pageable) {
        return service.publicReviews(productId, pageable);
    }
    @GetMapping("/products/{productId}/rating")
    public ProductRatingResponse rating(@PathVariable UUID productId) {
        return service.rating(productId);
    }
    @GetMapping("/pending")
    public Page<ReviewResponse> pending(Pageable pageable) { return service.pending(pageable); }
    @PatchMapping("/{id}/approve")
    public ReviewResponse approve(@PathVariable UUID id, Authentication authentication) { return service.moderate(id, true, authentication.getName()); }
    @PatchMapping("/{id}/reject")
    public ReviewResponse reject(@PathVariable UUID id, Authentication authentication) { return service.moderate(id, false, authentication.getName()); }
}
