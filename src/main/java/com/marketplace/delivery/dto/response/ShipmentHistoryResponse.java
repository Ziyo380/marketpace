package com.marketplace.delivery.dto.response;

import com.marketplace.delivery.entity.ShipmentActorType;
import com.marketplace.delivery.entity.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

public record ShipmentHistoryResponse(
        UUID id,
        UUID shipmentId,
        ShipmentStatus fromStatus,
        ShipmentStatus toStatus,
        ShipmentActorType actorType,
        String actorUsername,
        Instant changedAt,
        String details
) {}
