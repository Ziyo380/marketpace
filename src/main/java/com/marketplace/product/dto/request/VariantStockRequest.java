package com.marketplace.product.dto.request;

import jakarta.validation.constraints.Positive;

public record VariantStockRequest(@Positive int amount) {}
