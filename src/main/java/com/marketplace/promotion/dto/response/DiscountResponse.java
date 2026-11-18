package com.marketplace.promotion.dto.response;

import java.math.BigDecimal;

public record DiscountResponse(String code, BigDecimal subtotalAmount,
                               BigDecimal discountAmount, BigDecimal totalAmount) {}
