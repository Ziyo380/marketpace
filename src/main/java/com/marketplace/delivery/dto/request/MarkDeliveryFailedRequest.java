package com.marketplace.delivery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkDeliveryFailedRequest(
        @NotBlank @Size(max = 255) String failureReason
) {
}
