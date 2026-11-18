package com.marketplace.admin.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, String actor, String action, String targetType,
                               UUID targetId, String details, Instant createdAt) {}
