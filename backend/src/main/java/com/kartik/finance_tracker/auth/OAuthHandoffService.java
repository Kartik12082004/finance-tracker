package com.kartik.finance_tracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.auth.refresh.RefreshTokenService;
import com.kartik.finance_tracker.users.User;

@Service
public class OAuthHandoffService {

    private static final int CODE_BYTES = 32;
    private static final long CODE_EXPIRATION_SECONDS = 60;

    private final OAuthHandoffCodeRepository handoffCodeRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthHandoffService(
            OAuthHandoffCodeRepository handoffCodeRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.handoffCodeRepository = handoffCodeRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public String createHandoffCode(User user, String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "An authenticated browser session is required"
            );
        }

        byte[] randomBytes = new byte[CODE_BYTES];
        secureRandom.nextBytes(randomBytes);

        String rawCode = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        OffsetDateTime now = OffsetDateTime.now();

        OAuthHandoffCode handoffCode = new OAuthHandoffCode(
                user,
                hash(rawCode),
                hash(sessionId),
                now.plusSeconds(CODE_EXPIRATION_SECONDS)
        );

        handoffCodeRepository.save(handoffCode);

        // Return the raw code only to the OAuth success handler.
        // The database stores only its hash.
        return rawCode;
    }

    @Transactional
    public AuthResponse exchangeCode(
            String rawCode,
            String sessionId
    ) {
        if (rawCode == null || rawCode.isBlank()
                || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid or expired OAuth handoff code"
            );
        }

        String codeHash = hash(rawCode);
        String sessionIdHash = hash(sessionId);
        OffsetDateTime now = OffsetDateTime.now();

        int consumed = handoffCodeRepository.consumeIfValid(
                codeHash,
                sessionIdHash,
                now
        );

        if (consumed != 1) {
            throw new IllegalArgumentException(
                    "Invalid or expired OAuth handoff code"
            );
        }

        OAuthHandoffCode handoffCode = handoffCodeRepository
                .findByCodeHash(codeHash)
                .orElseThrow(() -> new IllegalStateException(
                        "Consumed OAuth handoff code could not be loaded"
                ));

        User user = handoffCode.getUser();

        String accessToken =
                jwtService.generateAccessToken(user.getId());

        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                accessToken,
                jwtService.getAccessTokenExpiration(),
                refreshToken
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to hash OAuth handoff value",
                    exception
            );
        }
    }
}
