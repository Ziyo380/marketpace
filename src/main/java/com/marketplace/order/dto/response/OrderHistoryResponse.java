package com.marketplace.order.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(UUID id, UUID sellerOrderId, String previousStatus,
                                   String newStatus, String actor, String reason, Instant createdAt) {}
