package com.marketplace.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@Size(max = 100) String fullName,
                                   @Size(max = 30) String phone) {}
