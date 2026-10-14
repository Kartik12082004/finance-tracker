package com.kartik.finance_tracker.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginFailureHandlerTest {

    private static final String CALLBACK_URL =
            "http://localhost:5173/oauth/callback";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private AuthenticationException exception;

    private OAuth2LoginFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        failureHandler = new OAuth2LoginFailureHandler();
    }

    @Test
    void onAuthenticationFailure_shouldRedirectToLoginErrorWhenNoLinkIntent()
            throws Exception {

        when(request.getSession(false)).thenReturn(null);

        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        verify(response).sendRedirect(
                CALLBACK_URL + "#error=oauth_login_failed"
        );
    }

    @Test
    void onAuthenticationFailure_shouldClearLinkIntentAndRedirectToLinkError()
            throws Exception {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        )).thenReturn("user-id");
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        )).thenReturn("GITHUB");

        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        );
        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        );
        verify(response).sendRedirect(
                CALLBACK_URL + "#error=github_link_failed"
        );
    }

    @Test
    void onAuthenticationFailure_shouldDetectLinkIntentFromProviderAttribute()
            throws Exception {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        )).thenReturn(null);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        )).thenReturn("GITHUB");

        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        );
        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        );
        verify(response).sendRedirect(
                CALLBACK_URL + "#error=github_link_failed"
        );
    }

    @Test
    void onAuthenticationFailure_shouldRedirectToGoogleLinkError()
            throws Exception {

        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        )).thenReturn("user-id");
        when(session.getAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        )).thenReturn("GOOGLE");

        failureHandler.onAuthenticationFailure(
                request,
                response,
                exception
        );

        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        );
        verify(session).removeAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        );
        verify(response).sendRedirect(
                CALLBACK_URL + "#error=google_link_failed"
        );
    }

}
