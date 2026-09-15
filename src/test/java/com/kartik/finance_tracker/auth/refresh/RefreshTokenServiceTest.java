package com.kartik.finance_tracker.auth.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.kartik.finance_tracker.users.User;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                30
        );
    }

    @Test
    void createRefreshToken_shouldGenerateAndStoreRefreshToken() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(user);

        assertThat(rawToken)
                .isNotBlank();

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        // The refresh token must belong to the authenticated user.
        assertThat(savedToken.getUser())
                .isSameAs(user);

        // Only the hash should be stored, never the plaintext refresh token.
        assertThat(savedToken.getTokenHash())
                .isNotEqualTo(rawToken);

        // SHA-256 represented as hexadecimal contains exactly 64 characters.
        assertThat(savedToken.getTokenHash())
                .hasSize(64);

        assertThat(savedToken.getExpiresAt())
                .isAfter(savedToken.getCreatedAt());
    }

    @Test
    void createRefreshToken_shouldSetExpirationApproximatelyThirtyDaysAhead() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime beforeCreation = OffsetDateTime.now();

        refreshTokenService.createRefreshToken(user);

        OffsetDateTime afterCreation = OffsetDateTime.now();

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();

        // Refresh tokens should remain usable for approximately 30 days.
        assertThat(savedToken.getExpiresAt())
                .isBetween(
                        beforeCreation.plusDays(30),
                        afterCreation.plusDays(30)
                );
    }

    @Test
    void createRefreshToken_shouldGenerateDifferentTokens() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String firstToken =
                refreshTokenService.createRefreshToken(user);

        String secondToken =
                refreshTokenService.createRefreshToken(user);

        // Each refresh credential must be unpredictable and unique.
        assertThat(secondToken)
                .isNotEqualTo(firstToken);
    }

    @Test
    void createRefreshToken_shouldStoreHashRatherThanPlaintext() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken =
                refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        String storedHash =
                captor.getValue().getTokenHash();

        // A database compromise must not reveal the usable refresh token.
        assertThat(storedHash)
                .doesNotContain(rawToken);

        assertThat(storedHash)
                .hasSize(64);
    }

    @Test
    void validateAndGet_shouldReturnValidRefreshToken() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        RefreshToken refreshToken = new RefreshToken(
                user,
                "stored-token-hash",
                OffsetDateTime.now().plusDays(30)
        );

        when(refreshTokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.of(refreshToken));

        RefreshToken result =
                refreshTokenService.validateAndGet("raw-refresh-token");

        // A valid token should resolve to its database record.
        assertThat(result)
                .isSameAs(refreshToken);
    }

    @Test
    void validateAndGet_shouldRejectUnknownToken() {

        when(refreshTokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.empty());

        // An unknown refresh credential must never be accepted.
        assertThatThrownBy(() ->
                refreshTokenService.validateAndGet("unknown-token")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void validateAndGet_shouldRejectRevokedToken() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        RefreshToken refreshToken = new RefreshToken(
                user,
                "stored-token-hash",
                OffsetDateTime.now().plusDays(30)
        );

        refreshToken.setRevokedAt(OffsetDateTime.now());

        when(refreshTokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.of(refreshToken));

        // A previously revoked token must never be reusable.
        assertThatThrownBy(() ->
                refreshTokenService.validateAndGet("revoked-token")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void validateAndGet_shouldRejectExpiredToken() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        RefreshToken refreshToken = new RefreshToken(
                user,
                "stored-token-hash",
                OffsetDateTime.now().minusSeconds(1)
        );

        when(refreshTokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.of(refreshToken));

        // An expired token must not be accepted even if it exists in the database.
        assertThatThrownBy(() ->
                refreshTokenService.validateAndGet("expired-token")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void rotateRefreshToken_shouldRevokeOldTokenAndCreateReplacement() {

        User user = new User(
                "test@example.com",
                "hashed-password",
                "Test User"
        );

        RefreshToken oldToken = new RefreshToken(
                user,
                "stored-token-hash",
                OffsetDateTime.now().plusDays(30)
        );

        when(refreshTokenRepository.findByTokenHash(any(String.class)))
                .thenReturn(Optional.of(oldToken));

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenRotation rotation =
                refreshTokenService.rotateRefreshToken("old-refresh-token");

        assertThat(rotation)
                .isNotNull();

        // Rotation must preserve the user associated with the original token.
        assertThat(rotation.user())
                .isSameAs(user);

        // A replacement refresh credential must be generated.
        assertThat(rotation.refreshToken())
                .isNotBlank();

        // The old refresh token must be revoked during rotation.
        assertThat(oldToken.getRevokedAt())
                .isNotNull();

        // The replacement token must also be persisted.
        verify(refreshTokenRepository, org.mockito.Mockito.times(2))
                .save(any(RefreshToken.class));
    }
}
