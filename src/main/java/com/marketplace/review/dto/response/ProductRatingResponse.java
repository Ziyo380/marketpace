package com.marketplace.review.dto.response;

import java.util.UUID;

public record ProductRatingResponse(UUID productId, long count, double averageRating) {}
