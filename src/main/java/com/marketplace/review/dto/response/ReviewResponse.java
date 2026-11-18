package com.marketplace.review.dto.response;

import com.marketplace.review.entity.ReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(UUID id, UUID productId, String username, int rating,
                             String comment, ReviewStatus status, Instant createdAt) {}
