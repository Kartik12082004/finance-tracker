package com.kartik.finance_tracker.auth;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccountService;
    private final GitHubEmailService gitHubEmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2LoginSuccessHandler(
            OAuthAccountService oauthAccountService,
            GitHubEmailService gitHubEmailService,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.oauthAccountService = oauthAccountService;
        this.gitHubEmailService = gitHubEmailService;
        this.authorizedClientService = authorizedClientService;
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

        OAuthProvider provider;
        String providerUserId;
        String email;
        String name;

        if ("google".equals(registrationId)) {

            provider = OAuthProvider.GOOGLE;

            // Google uses "sub" as the stable user identifier.
            providerUserId = oauthUser.getAttribute("sub");

            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("name");

        } else if ("github".equals(registrationId)) {

            provider = OAuthProvider.GITHUB;

            // GitHub uses "id" as the stable user identifier.
            Object githubUserId = oauthUser.getAttribute("id");
            providerUserId = String.valueOf(githubUserId);

            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("name");

            // GitHub may hide the email from the normal profile response.
            if (email == null || email.isBlank()) {

                OAuth2AuthorizedClient authorizedClient =
                        authorizedClientService.loadAuthorizedClient(
                                registrationId,
                                oauthToken.getName()
                        );

                if (authorizedClient == null) {
                    throw new IllegalStateException(
                            "GitHub OAuth client could not be loaded"
                    );
                }

                email = gitHubEmailService.findVerifiedEmail(
                        authorizedClient.getAccessToken().getTokenValue()
                );
            }

            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "No verified GitHub email address is available"
                );
            }

        } else {
            throw new IllegalArgumentException(
                    "Unsupported OAuth provider: " + registrationId
            );
        }

        oauthAccountService.findOrCreateUser(
                provider,
                providerUserId,
                email,
                name
        );

        // Root is a temporary destination until the React frontend exists.
        response.sendRedirect("/");
    }
}