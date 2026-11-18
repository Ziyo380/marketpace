package com.marketplace.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductVariantRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,80}") String sku,
        @NotNull @DecimalMin("0.01") BigDecimal price) {}
