package com.marketplace.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSellerRequest(
        @NotBlank
        @Size(max = 500)
        String rejectionReason
) {
}
