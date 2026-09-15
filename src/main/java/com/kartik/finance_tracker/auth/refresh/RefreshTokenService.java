package com.kartik.finance_tracker.auth.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.kartik.finance_tracker.users.User;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;
    private final long refreshTokenExpirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureRandom = new SecureRandom();
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String createRefreshToken(User user) {

        // Generate a cryptographically secure random token.
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        // URL-safe encoding makes the token safe to transport in JSON and HTTP.
        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // Only the hash is persisted. The plaintext token is returned to the client.
        String tokenHash = hashToken(rawToken);

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusDays(refreshTokenExpirationDays);

        RefreshToken refreshToken = new RefreshToken(
                user,
                tokenHash,
                expiresAt
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public RefreshToken validateAndGet(String rawToken) {

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        // A revoked refresh token can no longer be used.
        if (refreshToken.getRevokedAt() != null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        // An expired refresh token can no longer be used.
        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        return refreshToken;
    }

   public RefreshTokenRotation rotateRefreshToken(String rawToken) {

        RefreshToken oldToken = validateAndGet(rawToken);

        // Revoke the old refresh token before issuing its replacement.
        oldToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(oldToken);

        // The replacement belongs to the same Finance Tracker user.
        String newRefreshToken = createRefreshToken(oldToken.getUser());

        return new RefreshTokenRotation(
                oldToken.getUser(),
                newRefreshToken
        );
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            // Store the SHA-256 hash as a 64-character hexadecimal string.
            StringBuilder hex = new StringBuilder(hash.length * 2);

            for (byte value : hash) {
                hex.append(String.format("%02x", value));
            }

            return hex.toString();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash refresh token",
                    exception
            );
        }
    }
}
