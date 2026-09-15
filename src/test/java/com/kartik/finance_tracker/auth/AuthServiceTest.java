package com.kartik.finance_tracker.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.dto.LoginRequest;
import com.kartik.finance_tracker.auth.dto.RegisterRequest;
import com.kartik.finance_tracker.auth.jwt.JwtService;
import com.kartik.finance_tracker.auth.refresh.RefreshTokenRotation;
import com.kartik.finance_tracker.auth.refresh.RefreshTokenService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService
        );
    }

    @Test
    void register_shouldCreateUserWithHashedPasswordAndTokens() {

        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "password123",
                "Test User"
        );

        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
                .thenReturn("hashed-password");

        User savedUser = new User(
                request.email(),
                "hashed-password",
                request.name()
        );

        // Give the saved user a stable ID for verifying the response.
        savedUser.setId(userId);

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(jwtService.generateAccessToken(userId))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenExpiration())
                .thenReturn(900L);

        when(refreshTokenService.createRefreshToken(savedUser))
                .thenReturn("test-refresh-token");

        AuthResponse response = authService.register(request);

        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        assertEquals("test-access-token", response.accessToken());
        assertEquals(900L, response.expiresIn());
        assertEquals("test-refresh-token", response.refreshToken());

        // The raw password must never be passed to the repository.
        verify(passwordEncoder).encode("password123");

        // Registration must persist the newly created user.
        verify(userRepository).save(any(User.class));

        // A successful registration should issue an access token.
        verify(jwtService).generateAccessToken(userId);

        // A successful registration should also issue a refresh token.
        verify(refreshTokenService).createRefreshToken(savedUser);
    }

    @Test
    void register_shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "existing@example.com",
                "password123",
                "Existing User"
        );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "Email is already registered",
                exception.getMessage()
        );

        // No password should be hashed or user persisted when the email already exists.
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));

        // Failed registration must not issue either type of token.
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void login_shouldAuthenticateValidCredentialsAndIssueTokens() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        User user = new User(
                request.email(),
                "stored-hash",
                "Test User"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user.getId()))
                .thenReturn("test-access-token");

        when(jwtService.getAccessTokenExpiration())
                .thenReturn(900L);

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn("test-refresh-token");

        AuthResponse response = authService.login(request);

        assertEquals(user.getId(), response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        assertEquals("test-access-token", response.accessToken());
        assertEquals(900L, response.expiresIn());
        assertEquals("test-refresh-token", response.refreshToken());

        verify(passwordEncoder).matches(
                "password123",
                "stored-hash"
        );

        // Successful authentication should issue an access token.
        verify(jwtService).generateAccessToken(user.getId());

        // Successful authentication should also issue a refresh token.
        verify(refreshTokenService).createRefreshToken(user);
    }

    @Test
    void login_shouldRejectUnknownEmail() {

        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "password123"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void login_shouldRejectIncorrectPassword() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        User user = new User(
                request.email(),
                "stored-hash",
                "Test User"
        );

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        // A failed password check must never result in either token.
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void refresh_shouldRotateRefreshTokenAndIssueNewAccessToken() {

        User user = new User(
                "test@example.com",
                "stored-hash",
                "Test User"
        );

        RefreshTokenRotation rotation = new RefreshTokenRotation(
                user,
                "new-refresh-token"
        );

        when(refreshTokenService.rotateRefreshToken("old-refresh-token"))
                .thenReturn(rotation);

        when(jwtService.generateAccessToken(user.getId()))
                .thenReturn("new-access-token");

        when(jwtService.getAccessTokenExpiration())
                .thenReturn(900L);

        AuthResponse response =
                authService.refresh("old-refresh-token");

        assertEquals(user.getId(), response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());
        assertEquals("new-access-token", response.accessToken());
        assertEquals(900L, response.expiresIn());
        assertEquals("new-refresh-token", response.refreshToken());

        // The supplied refresh token must be validated and rotated.
        verify(refreshTokenService)
                .rotateRefreshToken("old-refresh-token");

        // A successful refresh must issue a new access token.
        verify(jwtService)
                .generateAccessToken(user.getId());

        verify(jwtService)
                .getAccessTokenExpiration();
    }

    @Test
    void refresh_shouldRejectInvalidRefreshToken() {

        when(refreshTokenService.rotateRefreshToken("invalid-refresh-token"))
                .thenThrow(new IllegalArgumentException("Invalid refresh token"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh("invalid-refresh-token")
        );

        assertEquals(
                "Invalid refresh token",
                exception.getMessage()
        );

        // An invalid refresh token must never result in a new access token.
        verify(jwtService, never()).generateAccessToken(any());

        // The service should not attempt to retrieve token expiration
        // when refresh-token validation fails.
        verify(jwtService, never()).getAccessTokenExpiration();
    }
}
