package com.kartik.finance_tracker.auth;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginFailureHandler
        implements AuthenticationFailureHandler {

    private static final String FRONTEND_CALLBACK =
            "http://localhost:5173/oauth/callback";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        HttpSession session = request.getSession(false);

        boolean hasLinkIntent = session != null
                && (session.getAttribute(
                            OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
                    ) != null
                    || session.getAttribute(
                            OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
                    ) != null);

        if (hasLinkIntent) {
            Object intendedProvider = session.getAttribute(
                    OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
            );

            String error = OAuthProvider.GITHUB.name().equals(intendedProvider)
                    ? "github_link_failed"
                    : OAuthProvider.GOOGLE.name().equals(intendedProvider)
                    ? "google_link_failed"
                    : "account_link_failed";

            session.removeAttribute(
                    OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
            );
            session.removeAttribute(
                    OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
            );

            response.sendRedirect(
                    FRONTEND_CALLBACK + "#error=" + error
            );
            return;
        }

        if (isEmailAlreadyRegistered(exception)) {
            response.sendRedirect(
                    FRONTEND_CALLBACK + "#error=oauth_email_exists"
            );
            return;
        }

        response.sendRedirect(
                FRONTEND_CALLBACK + "#error=oauth_login_failed"
        );
    }

    private boolean isEmailAlreadyRegistered(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof OAuthEmailAlreadyRegisteredException) {
                return true;
            }

            Throwable cause = current.getCause();

            if (cause == current) {
                break;
            }

            current = cause;
        }

        return false;
    }
}
