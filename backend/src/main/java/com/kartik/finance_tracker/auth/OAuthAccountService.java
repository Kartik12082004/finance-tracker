package com.kartik.finance_tracker.auth;

import com.kartik.finance_tracker.categories.CategoryService;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public OAuthAccountService(
            OAuthAccountRepository oauthAccountRepository,
            UserRepository userRepository,
            CategoryService categoryService
    ) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public User findOrCreateUser(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name
    ) {
        Optional<OAuthAccount> existingAccount =
                oauthAccountRepository.findByProviderAndProviderUserId(
                        provider, providerUserId);

        if (existingAccount.isPresent()) {
                User user = existingAccount.get().getUser();
                categoryService.ensureDefaultCategories(user);
                return user;
        }

        return createOAuthUser(provider, providerUserId, email, name);
    }

    @Transactional
    public void linkOAuthAccount(
            User user,
            OAuthProvider provider,
            String providerUserId
    ) {
        var existingAccount = oauthAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId);

        if (existingAccount.isPresent()) {
            if (existingAccount.get().getUser().getId().equals(user.getId())) {
                return;
            }

            throw new IllegalArgumentException(
                    "This OAuth account is already linked to another Finance Tracker account."
            );
        }

        OAuthAccount oauthAccount = new OAuthAccount(
                user,
                provider,
                providerUserId
        );

        oauthAccountRepository.save(oauthAccount);
    }

    @Transactional
    public void unlinkOAuthAccount(
            User user,
            OAuthProvider provider
    ) {
        UUID userId = user.getId();

        List<OAuthAccount> linkedAccounts =
                oauthAccountRepository.findAllByUserId(userId);

        List<OAuthAccount> accountsToRemove = linkedAccounts.stream()
                .filter(account -> account.getProvider() == provider)
                .toList();

        if (accountsToRemove.isEmpty()) {
            throw new NoSuchElementException(
                    "No " + provider.name() + " account is linked."
            );
        }

        boolean hasPassword = user.getPasswordHash() != null
                && !user.getPasswordHash().isBlank();

        boolean hasAnotherProvider = linkedAccounts.stream()
                .anyMatch(account -> account.getProvider() != provider);

        if (!hasPassword && !hasAnotherProvider) {
            throw new IllegalStateException(
                    "You cannot disconnect your only sign-in method. "
                            + "Link another provider or set a password first."
            );
        }

        oauthAccountRepository.deleteAll(accountsToRemove);
    }

    private User createOAuthUser(
            OAuthProvider provider,
            String providerUserId,
            String email,
            String name
    ) {
        // Never automatically link accounts based only on matching email.
        if (userRepository.existsByEmail(email)) {
                throw new OAuthEmailAlreadyRegisteredException(
                        "An account with this email already exists. Log in with that account first."
                );
        }

        User user = new User(email, null, name);
        User savedUser = userRepository.save(user);

        categoryService.ensureDefaultCategories(savedUser);

        OAuthAccount oauthAccount = new OAuthAccount(
                savedUser,
                provider,
                providerUserId
        );

        oauthAccountRepository.save(oauthAccount);

        return savedUser;
    }
}
