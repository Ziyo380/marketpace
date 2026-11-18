package com.marketplace.user.dto.response;

import com.marketplace.user.entity.UserStatus;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        UserStatus status,
        String fullName,
        String phone
) {}
