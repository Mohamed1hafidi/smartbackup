package com.smartbackup.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        UserSummary user
) {
}
