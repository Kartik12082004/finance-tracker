package com.kartik.finance_tracker.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.auth.refresh.RefreshTokenService;
import com.kartik.finance_tracker.users.User;

@ExtendWith(MockitoExtension.class)
class OAuthHandoffServiceTest {

    @Mock
    private OAuthHandoffCodeRepository handoffCodeRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private OAuthHandoffService oauthHandoffService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", null, "Test User");
    }

    @Test
    void createHandoffCode_shouldGenerateRandomCodeAndStoreOnlyHashes() throws Exception {
        when(handoffCodeRepository.save(any(OAuthHandoffCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String code = oauthHandoffService.createHandoffCode(user, "session-123");

        assertNotNull(code);
        assertFalse(code.isBlank());
        assertNotEquals("session-123", code);

        ArgumentCaptor<OAuthHandoffCode> captor =
                ArgumentCaptor.forClass(OAuthHandoffCode.class);

        verify(handoffCodeRepository).save(captor.capture());

        OAuthHandoffCode savedCode = captor.getValue();

        assertEquals(user, savedCode.getUser());
        assertEquals(sha256(code), savedCode.getCodeHash());
        assertEquals(sha256("session-123"), savedCode.getSessionIdHash());
        assertNotEquals(code, savedCode.getCodeHash());
        assertNotEquals("session-123", savedCode.getSessionIdHash());
        assertTrue(savedCode.getExpiresAt().isAfter(OffsetDateTime.now()));
        assertNull(savedCode.getConsumedAt());
    }

    @Test
    void createHandoffCode_shouldRejectBlankSessionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> oauthHandoffService.createHandoffCode(user, " ")
        );

        verifyNoInteractions(handoffCodeRepository);
    }

    @Test
    void exchangeCode_shouldReturnAuthResponseForValidCode() {
        String rawCode = "valid-handoff-code";
        String sessionId = "session-123";
        String codeHash = sha256(rawCode);
        OffsetDateTime now = OffsetDateTime.now();

        OAuthHandoffCode handoffCode = new OAuthHandoffCode(
                user,
                codeHash,
                sha256(sessionId),
                now.plusSeconds(60)
        );

        when(handoffCodeRepository.consumeIfValid(
                eq(codeHash),
                eq(sha256(sessionId)),
                any(OffsetDateTime.class)
        )).thenReturn(1);

        when(handoffCodeRepository.findByCodeHash(codeHash))
                .thenReturn(Optional.of(handoffCode));

        when(jwtService.generateAccessToken(user.getId()))
                .thenReturn("access-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user))
                .thenReturn("refresh-token");

        AuthResponse response =
                oauthHandoffService.exchangeCode(rawCode, sessionId);

        assertNotNull(response);
        verify(handoffCodeRepository).consumeIfValid(
                eq(codeHash),
                eq(sha256(sessionId)),
                any(OffsetDateTime.class)
        );
        verify(jwtService).generateAccessToken(user.getId());
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void exchangeCode_shouldRejectCodeThatCannotBeConsumed() {
        when(handoffCodeRepository.consumeIfValid(
                anyString(),
                anyString(),
                any(OffsetDateTime.class)
        )).thenReturn(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> oauthHandoffService.exchangeCode(
                        "invalid-code", "session-123"
                )
        );

        verify(handoffCodeRepository, never()).findByCodeHash(anyString());
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    void exchangeCode_shouldRejectBlankCodeOrSessionId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> oauthHandoffService.exchangeCode("", "session-123")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> oauthHandoffService.exchangeCode("valid-code", " ")
        );

        verifyNoInteractions(handoffCodeRepository, jwtService, refreshTokenService);
    }

    @Test
    void exchangeCode_shouldRejectCodeWhenConsumedCodeCannotBeLoaded() {
        String rawCode = "valid-handoff-code";

        when(handoffCodeRepository.consumeIfValid(
                anyString(),
                anyString(),
                any(OffsetDateTime.class)
        )).thenReturn(1);

        when(handoffCodeRepository.findByCodeHash(sha256(rawCode)))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> oauthHandoffService.exchangeCode(rawCode, "session-123")
        );

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    private String sha256(String value) {
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
