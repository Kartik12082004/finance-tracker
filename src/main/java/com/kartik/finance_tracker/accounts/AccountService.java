package com.kartik.finance_tracker.accounts;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import java.util.UUID;

import org.springframework.stereotype.Service;

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

    public Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String currency
    ) {

        // Verify that the account is being created for an existing user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = new Account(
                user,
                name,
                type,
                currency
        );

        return accountRepository.save(account);
    }
}