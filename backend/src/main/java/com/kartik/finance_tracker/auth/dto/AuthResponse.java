package com.kartik.finance_tracker.auth.dto;

import java.util.UUID;

public record AuthResponse(
        UUID userId,
        String email,
        String name,
        String accessToken,
        long expiresIn,
        String refreshToken
) {
}
