package com.marketplace.seller.dto.response;

import com.marketplace.seller.entity.SellerStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SellerResponse(
        UUID id,

        UUID userId,

        SellerStatus status,

        LocalDateTime approvedAt,

        String rejectionReason
) {
}
