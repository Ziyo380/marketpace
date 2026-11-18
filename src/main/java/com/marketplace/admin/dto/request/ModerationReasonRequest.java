package com.marketplace.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationReasonRequest(@NotBlank @Size(max = 500) String reason) {}
