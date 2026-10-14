package com.kartik.finance_tracker.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    private static final String SESSION_ID = "test-session-id";
    private static final String HANDOFF_CODE = "test-handoff-code";
    private static final String CALLBACK_URL =
            "http://localhost:5173/oauth/callback#code=" + HANDOFF_CODE;

    @Mock
    private OAuthAccountService oauthAccountService;

    @Mock
    private GitHubEmailService gitHubEmailService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private OAuth2User oauthUser;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private OAuthHandoffService oauthHandoffService;

    @Mock
    private HttpSession session;

    @Mock
    private UserRepository userRepository;

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(
                oauthAccountService,
                gitHubEmailService,
                authorizedClientService,
                oauthHandoffService,
                userRepository
        );
    }

    private User stubSuccessfulHandoff(String email, String name) {
        User user = new User(email, null, name);

        when(oauthAccountService.findOrCreateUser(
                any(OAuthProvider.class),
                anyString(),
                eq(email),
                eq(name)
        )).thenReturn(user);

        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(session.getId()).thenReturn(SESSION_ID);

        when(oauthHandoffService.createHandoffCode(user, SESSION_ID))
                .thenReturn(HANDOFF_CODE);

        return user;
    }

    private void stubGoogleProfile() {
        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("google");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("sub")).thenReturn("google-user-123");
        when(oauthUser.getAttribute("email")).thenReturn("test@example.com");
        when(oauthUser.getAttribute("name")).thenReturn("Test User");
    }

    private void stubGitHubProfile(String email, String name) {
        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("id")).thenReturn(123456);
        when(oauthUser.getAttribute("email")).thenReturn(email);
        when(oauthUser.getAttribute("name")).thenReturn(name);
    }

    private void verifySuccessfulHandoff(User user) throws Exception {
        verify(oauthHandoffService).createHandoffCode(user, SESSION_ID);
        verify(response).sendRedirect(CALLBACK_URL);
    }

    @Test
    void onAuthenticationSuccess_shouldMapGoogleIdentityToUser()
            throws Exception {

        stubGoogleProfile();

        User user = stubSuccessfulHandoff(
                "test@example.com",
                "Test User"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GOOGLE,
                "google-user-123",
                "test@example.com",
                "Test User"
        );

        verifySuccessfulHandoff(user);
    }

    @Test
    void onAuthenticationSuccess_shouldMapGitHubIdentityWithProfileEmail()
            throws Exception {

        stubGitHubProfile("github@example.com", "GitHub User");

        User user = stubSuccessfulHandoff(
                "github@example.com",
                "GitHub User"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GITHUB,
                "123456",
                "github@example.com",
                "GitHub User"
        );

        verifySuccessfulHandoff(user);
    }

    @Test
    void onAuthenticationSuccess_shouldRetrieveGitHubEmailWhenProfileEmailMissing()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");
        when(authentication.getName()).thenReturn("github-user-123");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("id")).thenReturn(123456);
        when(oauthUser.getAttribute("email")).thenReturn(null);
        when(oauthUser.getAttribute("name")).thenReturn("GitHub User");

        when(authorizedClientService.loadAuthorizedClient(
                "github",
                "github-user-123"
        )).thenReturn(authorizedClient);

        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("github-access-token");

        when(gitHubEmailService.findVerifiedEmail("github-access-token"))
                .thenReturn("verified@example.com");

        User user = stubSuccessfulHandoff(
                "verified@example.com",
                "GitHub User"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GITHUB,
                "123456",
                "verified@example.com",
                "GitHub User"
        );

        verifySuccessfulHandoff(user);
    }

    @Test
    void onAuthenticationSuccess_shouldRedirectToReactCallbackWithHandoffCode()
            throws Exception {

        stubGoogleProfile();

        User user = stubSuccessfulHandoff(
                "test@example.com",
                "Test User"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verifySuccessfulHandoff(user);
    }

    @Test
    void onAuthenticationSuccess_shouldUseGitHubLoginWhenProfileNameMissing()
            throws Exception {

        stubGitHubProfile("github@example.com", null);
        when(oauthUser.getAttribute("login")).thenReturn("github-user");

        User user = stubSuccessfulHandoff(
                "github@example.com",
                "github-user"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GITHUB,
                "123456",
                "github@example.com",
                "github-user"
        );

        verifySuccessfulHandoff(user);
    }

    @Test
    void onAuthenticationSuccess_shouldLinkGitHubToIntendedUserWithoutHandoff()
            throws Exception {

        UUID userId = UUID.randomUUID();
        User intendedUser = new User(
                "existing@example.com",
                "password-hash",
                "Existing User"
        );

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        )).thenReturn(userId.toString());
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        )).thenReturn(OAuthProvider.GITHUB.name());

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("id")).thenReturn(123456);

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(intendedUser));

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(oauthAccountService).linkOAuthAccount(
                intendedUser,
                OAuthProvider.GITHUB,
                "123456"
        );

        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        );
        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        );

        verify(response).sendRedirect(
                "http://localhost:5173/oauth/callback#linked=github"
        );

        verify(oauthHandoffService, never()).createHandoffCode(
                any(User.class),
                anyString()
        );
    }

    @Test
    void onAuthenticationSuccess_shouldRedirectWhenGitHubIdentityIsAlreadyLinkedElsewhere()
            throws Exception {

        UUID userId = UUID.randomUUID();
        User intendedUser = new User(
                "existing@example.com",
                "password-hash",
                "Existing User"
        );

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        )).thenReturn(userId.toString());
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        )).thenReturn(OAuthProvider.GITHUB.name());

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAttribute("id")).thenReturn(123456);

        when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(intendedUser));

        org.mockito.Mockito.doThrow(new IllegalArgumentException(
                "This OAuth account is already linked to another Finance Tracker account."
        )).when(oauthAccountService).linkOAuthAccount(
                intendedUser,
                OAuthProvider.GITHUB,
                "123456"
        );

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        verify(response).sendRedirect(
                "http://localhost:5173/oauth/callback#error=github_link_failed"
        );

        verify(oauthHandoffService, never()).createHandoffCode(
                any(User.class),
                anyString()
        );
    }


}
