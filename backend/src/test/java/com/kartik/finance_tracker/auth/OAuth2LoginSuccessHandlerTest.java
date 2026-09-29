package com.kartik.finance_tracker.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

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

    private OAuth2LoginSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(
                oauthAccountService,
                gitHubEmailService,
                authorizedClientService
        );
    }

    @Test
    void onAuthenticationSuccess_shouldMapGoogleIdentityToUser()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("google");

        when(authentication.getPrincipal())
                .thenReturn(oauthUser);

        when(oauthUser.getAttribute("sub"))
                .thenReturn("google-user-123");

        when(oauthUser.getAttribute("email"))
                .thenReturn("test@example.com");

        when(oauthUser.getAttribute("name"))
                .thenReturn("Test User");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // Google is mapped to our OAuth provider model.
        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GOOGLE,
                "google-user-123",
                "test@example.com",
                "Test User"
        );
    }

    @Test
    void onAuthenticationSuccess_shouldMapGitHubIdentityWithProfileEmail()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");

        when(authentication.getPrincipal())
                .thenReturn(oauthUser);

        // GitHub uses "id" as the stable provider user identifier.
        when(oauthUser.getAttribute("id"))
                .thenReturn(123456);

        when(oauthUser.getAttribute("email"))
                .thenReturn("github@example.com");

        when(oauthUser.getAttribute("name"))
                .thenReturn("GitHub User");

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
    }

    @Test
    void onAuthenticationSuccess_shouldRetrieveGitHubEmailWhenProfileEmailMissing()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");

        when(authentication.getName())
                .thenReturn("github-user-123");

        when(authentication.getPrincipal())
                .thenReturn(oauthUser);

        when(oauthUser.getAttribute("id"))
                .thenReturn(123456);

        // GitHub profile does not expose an email.
        when(oauthUser.getAttribute("email"))
                .thenReturn(null);

        when(oauthUser.getAttribute("name"))
                .thenReturn("GitHub User");

        when(authorizedClientService.loadAuthorizedClient(
                "github",
                "github-user-123"
        )).thenReturn(authorizedClient);

        when(authorizedClient.getAccessToken())
                .thenReturn(accessToken);

        when(accessToken.getTokenValue())
                .thenReturn("github-access-token");

        when(gitHubEmailService.findVerifiedEmail(
                "github-access-token"
        )).thenReturn("verified@example.com");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // The verified email retrieved from GitHub is used to create the user.
        verify(oauthAccountService).findOrCreateUser(
                OAuthProvider.GITHUB,
                "123456",
                "verified@example.com",
                "GitHub User"
        );
    }

    @Test
    void onAuthenticationSuccess_shouldRedirectToRoot()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("google");

        when(authentication.getPrincipal())
                .thenReturn(oauthUser);

        when(oauthUser.getAttribute("sub"))
                .thenReturn("google-user-123");

        when(oauthUser.getAttribute("email"))
                .thenReturn("test@example.com");

        when(oauthUser.getAttribute("name"))
                .thenReturn("Test User");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication
        );

        // Root is only a temporary destination until the React frontend exists.
        verify(response).sendRedirect("/");
    }

    @Test
    void onAuthenticationSuccess_shouldUseGitHubLoginWhenProfileNameMissing()
            throws Exception {

        when(authentication.getAuthorizedClientRegistrationId())
                .thenReturn("github");

        when(authentication.getPrincipal())
                .thenReturn(oauthUser);

        when(oauthUser.getAttribute("id"))
                .thenReturn(123456);

        when(oauthUser.getAttribute("email"))
                .thenReturn("github@example.com");

        // GitHub profile has no display name.
        when(oauthUser.getAttribute("name"))
                .thenReturn(null);

        // The GitHub username is used as the fallback name.
        when(oauthUser.getAttribute("login"))
                .thenReturn("github-user");

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
    }

}
