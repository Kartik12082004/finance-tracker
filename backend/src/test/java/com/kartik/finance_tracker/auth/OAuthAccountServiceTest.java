package com.kartik.finance_tracker.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    @Mock
    private UserRepository userRepository;

    private OAuthAccountService oauthAccountService;

    @BeforeEach
    void setUp() {
        oauthAccountService = new OAuthAccountService(
                oauthAccountRepository,
                userRepository
        );
    }

    @Test
    void findOrCreateUser_shouldReturnExistingUserForLinkedOAuthAccount() {

        User existingUser = new User(
                "test@example.com",
                null,
                "Test User"
        );

        OAuthAccount oauthAccount = new OAuthAccount(
                existingUser,
                OAuthProvider.GOOGLE,
                "google-user-123"
        );

        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-123"
        )).thenReturn(Optional.of(oauthAccount));

        User result = oauthAccountService.findOrCreateUser(
                OAuthProvider.GOOGLE,
                "google-user-123",
                "test@example.com",
                "Test User"
        );

        assertEquals(existingUser, result);

        // An already-linked OAuth identity must not create another user.
        verify(userRepository, never()).save(any(User.class));
        verify(oauthAccountRepository, never()).save(any(OAuthAccount.class));
    }

    @Test
    void findOrCreateUser_shouldCreateNewUserAndOAuthAccount() {

        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-123"
        )).thenReturn(Optional.empty());

        when(userRepository.existsByEmail("new@example.com"))
                .thenReturn(false);

        User savedUser = new User(
                "new@example.com",
                null,
                "New User"
        );

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = oauthAccountService.findOrCreateUser(
                OAuthProvider.GOOGLE,
                "google-user-123",
                "new@example.com",
                "New User"
        );

        assertEquals(savedUser, result);

        verify(userRepository).save(any(User.class));
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void findOrCreateUser_shouldRejectExistingEmailWithoutLinkedOAuthAccount() {

        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GOOGLE,
                "google-user-123"
        )).thenReturn(Optional.empty());

        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> oauthAccountService.findOrCreateUser(
                        OAuthProvider.GOOGLE,
                        "google-user-123",
                        "existing@example.com",
                        "Existing User"
                )
        );

        assertEquals(
                "An account with this email already exists. Log in with that account first.",
                exception.getMessage()
        );

        // Do not create or link anything when an existing account owns the email.
        verify(userRepository, never()).save(any(User.class));
        verify(oauthAccountRepository, never()).save(any(OAuthAccount.class));
    }

    @Test
    void findOrCreateUser_shouldSupportDifferentProvidersWithSameProviderUserId() {

        when(oauthAccountRepository.findByProviderAndProviderUserId(
                OAuthProvider.GITHUB,
                "123"
        )).thenReturn(Optional.empty());

        when(userRepository.existsByEmail("github@example.com"))
                .thenReturn(false);

        User savedUser = new User(
                "github@example.com",
                null,
                "GitHub User"
        );

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        User result = oauthAccountService.findOrCreateUser(
                OAuthProvider.GITHUB,
                "123",
                "github@example.com",
                "GitHub User"
        );

        assertEquals(savedUser, result);

        // Provider is part of the OAuth identity, so the same provider user ID
        // can legitimately exist under a different provider.
        verify(oauthAccountRepository).findByProviderAndProviderUserId(
                OAuthProvider.GITHUB,
                "123"
        );
    }
}
