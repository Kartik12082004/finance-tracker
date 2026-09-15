package com.kartik.finance_tracker.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.auth.dto.AuthResponse;
import com.kartik.finance_tracker.auth.dto.LoginRequest;
import com.kartik.finance_tracker.auth.dto.RefreshTokenRequest;
import com.kartik.finance_tracker.auth.dto.RegisterRequest;
import com.kartik.finance_tracker.common.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_shouldReturnCreatedUser() throws Exception {

        UUID userId = UUID.randomUUID();

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse(
                        userId,
                        "test@example.com",
                        "Test User",
                        "test-access-token",
                        900L,
                        "test-refresh-token"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void login_shouldReturnAuthenticatedUser() throws Exception {

        UUID userId = UUID.randomUUID();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse(
                        userId,
                        "test@example.com",
                        "Test User",
                        "test-access-token",
                        900L,
                        "test-refresh-token"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void register_shouldRejectMissingEmail() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "password123",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    void register_shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "password123",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    void register_shouldRejectShortPassword() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "short",
                                    "name": "Test User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Password must be between 8 and 100 characters"
                ));
    }

    @Test
    void register_shouldRejectMissingName() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Name is required"));
    }

    @Test
    void login_shouldRejectMissingEmail() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    void login_shouldRejectMissingPassword() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Password is required"));
    }

    @Test
    void login_shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email must be valid"));
    }

    @Test
    void register_shouldRejectDuplicateEmail() throws Exception {

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Email is already registered"
                ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "existing@example.com",
                                    "password": "password123",
                                    "name": "Existing User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Email is already registered"
                ));
    }

    @Test
    void login_shouldRejectInvalidCredentials() throws Exception {

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Invalid email or password"
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Invalid email or password"
                ));
    }

    @Test
    void refresh_shouldReturnNewTokenPair() throws Exception {

        UUID userId = UUID.randomUUID();

        when(authService.refresh("old-refresh-token"))
                .thenReturn(new AuthResponse(
                        userId,
                        "test@example.com",
                        "Test User",
                        "new-access-token",
                        900L,
                        "new-refresh-token"
                ));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        // A valid refresh request should be passed to the authentication service.
        verify(authService).refresh("old-refresh-token");
    }

    @Test
    void refresh_shouldRejectMissingRefreshToken() throws Exception {

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Refresh token is required"
                ));
    }

    @Test
    void refresh_shouldRejectBlankRefreshToken() throws Exception {

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "refreshToken": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Refresh token is required"
                ));
    }

    @Test
    void refresh_shouldRejectInvalidRefreshToken() throws Exception {

        when(authService.refresh("invalid-refresh-token"))
                .thenThrow(new IllegalArgumentException(
                        "Invalid refresh token"
                ));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "refreshToken": "invalid-refresh-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Invalid refresh token"
                ));
    }
}
