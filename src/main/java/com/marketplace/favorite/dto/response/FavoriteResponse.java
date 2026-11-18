package com.marketplace.favorite.dto.response;

import com.marketplace.product.dto.response.ProductResponse;
import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(UUID id, ProductResponse product, Instant createdAt) {}
