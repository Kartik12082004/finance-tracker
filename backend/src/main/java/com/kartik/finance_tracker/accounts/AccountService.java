package com.kartik.finance_tracker.accounts;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.investments.InvestmentTransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final InvestmentTransactionRepository investmentTransactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            InvestmentTransactionRepository investmentTransactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.investmentTransactionRepository = investmentTransactionRepository;
    }

    @Transactional
    public Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String currency,
            BigDecimal openingBalance
    ) {

        // Verify that the account is being created for an existing user.
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account account = new Account(
                user,
                name,
                type,
                currency,
                openingBalance
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

    @Transactional
    public Account updateAccount(
            UUID userId,
            UUID accountId,
            String name,
            AccountType type,
            String currency
    ) {

        // Only an account belonging to the authenticated user can be updated.
        Account account = accountRepository.findByIdAndUser_Id(
                accountId,
                userId
        ).orElseThrow(() ->
                new ResourceNotFoundException("Account not found"));

        // Account editing changes metadata only.
        // Opening balance is intentionally not modified here because it
        // represents the account's initial financial state.
        account.setName(name);
        account.setType(type);
        account.setCurrency(currency);
        account.setUpdatedAt(OffsetDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(UUID userId, UUID accountId) {

        // Only an account belonging to the authenticated user can be deleted.
        Account account = accountRepository.findByIdAndUser_Id(
                accountId,
                userId
        ).orElseThrow(() ->
                new ResourceNotFoundException("Account not found"));

        // Financial history must never be deleted implicitly with an account.
        boolean hasTransactions =
                transactionRepository
                        .existsByAccount_IdOrDestinationAccount_Id(
                                accountId,
                                accountId
                        );

        // Investment transactions also make the account part of financial history.
        boolean hasInvestmentTransactions =
                investmentTransactionRepository.existsByAccount_Id(accountId);

        if (hasTransactions || hasInvestmentTransactions) {
            throw new IllegalArgumentException(
                    "Account cannot be deleted because it has financial transactions"
            );
        }

        accountRepository.delete(account);
    }
}
