package com.kartik.finance_tracker.auth;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private static final String FRONTEND_CALLBACK =
            "http://localhost:5173/oauth/callback";

    private final OAuthAccountService oauthAccountService;
    private final GitHubEmailService gitHubEmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final OAuthHandoffService oauthHandoffService;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(
            OAuthAccountService oauthAccountService,
            GitHubEmailService gitHubEmailService,
            OAuth2AuthorizedClientService authorizedClientService,
            OAuthHandoffService oauthHandoffService,
            UserRepository userRepository
    ) {
        this.oauthAccountService = oauthAccountService;
        this.gitHubEmailService = gitHubEmailService;
        this.authorizedClientService = authorizedClientService;
        this.oauthHandoffService = oauthHandoffService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken =
                (OAuth2AuthenticationToken) authentication;

        OAuth2User oauthUser = oauthToken.getPrincipal();

        String registrationId =
                oauthToken.getAuthorizedClientRegistrationId();

        HttpSession session = request.getSession(false);

        boolean hasLinkIntent = session != null
                && (session.getAttribute(
                            OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
                    ) != null
                    || session.getAttribute(
                            OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
                    ) != null);

        // A pending link operation must never fall through into normal sign-in.
        if (hasLinkIntent) {
                Object intendedProvider = session.getAttribute(
                        OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
                );

                Object intendedUserId = session.getAttribute(
                        OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
                );

                OAuthProvider provider;

                if (OAuthProvider.GOOGLE.name().equals(intendedProvider)) {
                        provider = OAuthProvider.GOOGLE;
                } else if (OAuthProvider.GITHUB.name().equals(intendedProvider)) {
                        provider = OAuthProvider.GITHUB;
                } else {
                        clearLinkIntent(session);
                        response.sendRedirect(
                                FRONTEND_CALLBACK + "#error=account_link_failed"
                        );
                        return;
                }

                String expectedRegistrationId = provider.name().toLowerCase();

                if (!expectedRegistrationId.equals(registrationId)
                        || !(intendedUserId instanceof String userIdString)) {
                        String error = expectedRegistrationId + "_link_failed";
                        clearLinkIntent(session);
                        response.sendRedirect(FRONTEND_CALLBACK + "#error=" + error);
                        return;
                }

                if (provider == OAuthProvider.GOOGLE) {
                        linkGoogleAccount(
                                oauthUser, session, userIdString, response
                        );
                } else {
                        linkGitHubAccount(
                                oauthUser, oauthToken, session, userIdString,
                                request, response
                        );
                }

                return;
        }

        OAuthProvider provider;
        String providerUserId;
        String email;
        String name;

        if ("google".equals(registrationId)) {

            provider = OAuthProvider.GOOGLE;

            providerUserId = oauthUser.getAttribute("sub");
            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("name");

        } else if ("github".equals(registrationId)) {

            provider = OAuthProvider.GITHUB;

            Object githubUserId = oauthUser.getAttribute("id");
            if (githubUserId == null) {
                response.sendRedirect(
                        FRONTEND_CALLBACK + "#error=oauth_login_failed"
                );
                return;
            }

            providerUserId = String.valueOf(githubUserId);
            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("name");

            if (name == null || name.isBlank()) {
                name = oauthUser.getAttribute("login");
            }

            if (email == null || email.isBlank()) {
                OAuth2AuthorizedClient authorizedClient =
                        authorizedClientService.loadAuthorizedClient(
                                registrationId,
                                oauthToken.getName()
                        );

                if (authorizedClient == null) {
                    response.sendRedirect(
                            FRONTEND_CALLBACK + "#error=oauth_login_failed"
                    );
                    return;
                }

                email = gitHubEmailService.findVerifiedEmail(
                        authorizedClient.getAccessToken().getTokenValue()
                );
            }

            if (email == null || email.isBlank()) {
                response.sendRedirect(
                        FRONTEND_CALLBACK + "#error=oauth_login_failed"
                );
                return;
            }

        } else {
            response.sendRedirect(
                    FRONTEND_CALLBACK + "#error=oauth_login_failed"
            );
            return;
        }

        try {
            User user = oauthAccountService.findOrCreateUser(
                    provider,
                    providerUserId,
                    email,
                    name
            );

            HttpSession handoffSession = request.getSession(true);

            String code = oauthHandoffService.createHandoffCode(
                    user,
                    handoffSession.getId()
            );

            response.sendRedirect(
                    FRONTEND_CALLBACK + "#code=" + code
            );

        } catch (IllegalArgumentException exception) {
            String error = "An account with this email already exists. Log in with that account first."
                    .equals(exception.getMessage())
                    ? "oauth_email_exists"
                    : "oauth_login_failed";

            response.sendRedirect(
                    FRONTEND_CALLBACK + "#error=" + error
            );
        }
    }

    private void linkGoogleAccount(
                OAuth2User oauthUser,
                HttpSession session,
                String userIdString,
                HttpServletResponse response
        ) throws IOException {

        try {
                UUID userId = UUID.fromString(userIdString);

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "The account requesting the link no longer exists"
                        ));

                String providerUserId = oauthUser.getAttribute("sub");

                if (providerUserId == null || providerUserId.isBlank()) {
                throw new IllegalArgumentException(
                        "Google did not provide a user ID"
                );
                }

                oauthAccountService.linkOAuthAccount(
                        user,
                        OAuthProvider.GOOGLE,
                        providerUserId
                );

                clearLinkIntent(session);

                response.sendRedirect(
                        FRONTEND_CALLBACK + "#linked=google"
                );

        } catch (IllegalArgumentException exception) {
                clearLinkIntent(session);

                response.sendRedirect(
                        FRONTEND_CALLBACK + "#error=google_link_failed"
                );
        }
   }

    private void linkGitHubAccount(
            OAuth2User oauthUser,
            OAuth2AuthenticationToken oauthToken,
            HttpSession session,
            String userIdString,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            UUID userId = UUID.fromString(userIdString);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "The account requesting the link no longer exists"
                    ));

            Object githubUserId = oauthUser.getAttribute("id");

            if (githubUserId == null) {
                throw new IllegalArgumentException(
                        "GitHub did not provide a user ID"
                );
            }

            String providerUserId = String.valueOf(githubUserId);

            oauthAccountService.linkOAuthAccount(
                    user,
                    OAuthProvider.GITHUB,
                    providerUserId
            );

            clearLinkIntent(session);

            response.sendRedirect(
                    FRONTEND_CALLBACK + "#linked=github"
            );

        } catch (IllegalArgumentException exception) {
            clearLinkIntent(session);

            response.sendRedirect(
                    FRONTEND_CALLBACK + "#error=github_link_failed"
            );
        }
    }

    private void clearLinkIntent(HttpSession session) {
        session.removeAttribute(
                OAuthAccountLinkController.LINK_USER_ID_SESSION_KEY
        );
        session.removeAttribute(
                OAuthAccountLinkController.LINK_PROVIDER_SESSION_KEY
        );
    }
}
