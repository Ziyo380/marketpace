package com.marketplace.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String refreshToken

) {
}
