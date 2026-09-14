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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.dto.LoginRequest;
import com.kartik.finance_tracker.auth.dto.RegisterRequest;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void register_shouldCreateUserWithHashedPassword() {

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

        AuthResponse response = authService.register(request);

        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());

        // The raw password must never be passed to the repository.
        verify(passwordEncoder).encode("password123");

        // Registration must persist the newly created user.
        verify(userRepository).save(any(User.class));
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
    }

    @Test
    void login_shouldAuthenticateValidCredentials() {

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

        AuthResponse response = authService.login(request);

        assertEquals(user.getId(), response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("Test User", response.name());

        verify(passwordEncoder).matches(
                "password123",
                "stored-hash"
        );
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
    }
}