package com.marketplace.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductVariantRequest(@NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") BigDecimal price, boolean active) {}
