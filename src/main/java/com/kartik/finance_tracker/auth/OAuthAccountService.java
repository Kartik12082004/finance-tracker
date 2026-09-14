package com.kartik.finance_tracker.auth;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;

    public OAuthAccountService(
            OAuthAccountRepository oauthAccountRepository,
            UserRepository userRepository
    ) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
    }

    public User findOrCreateUser(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name
    ) {
        // If this OAuth identity is already linked, return its existing Finance Tracker user.
        return oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .orElseGet(() -> createOAuthUser(
                        provider,
                        providerUserId,
                        email,
                        name
                ));
    }

    private User createOAuthUser(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name
    ) {
        // Do not silently link an OAuth identity to an existing account based only on email.
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists. Log in with that account first."
            );
        }

        // OAuth-only users do not have a local password.
        User user = new User(
                email,
                null,
                name
        );

        User savedUser = userRepository.save(user);

        OAuthAccount oauthAccount = new OAuthAccount(
                savedUser,
                provider,
                providerUserId
        );

        oauthAccountRepository.save(oauthAccount);

        return savedUser;
    }
}
