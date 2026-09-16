package com.kartik.finance_tracker.accounts;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String currency
    ) {

        // Verify that the account is being created for an existing user.
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account account = new Account(
                user,
                name,
                type,
                currency
        );

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccounts(UUID userId) {

        // The authenticated user should exist in the database.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        // Only return accounts owned by the authenticated user.
        return accountRepository.findByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID userId, UUID accountId) {

        // Scope the lookup by both account ID and authenticated user ID.
        // This prevents one user from accessing another user's account
        // simply by changing the account ID in the URL.
        return accountRepository.findByIdAndUser_Id(accountId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));
    }
}
