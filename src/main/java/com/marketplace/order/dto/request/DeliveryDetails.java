package com.marketplace.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DeliveryDetails(
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[+0-9() .-]{7,30}$", message = "Phone number has an invalid format")
        String recipientPhone,
        @NotBlank @Size(max = 32) String region,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 500) String addressLine
) {
}
