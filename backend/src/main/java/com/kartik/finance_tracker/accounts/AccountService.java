package com.kartik.finance_tracker.accounts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.investments.InvestmentTransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class AccountService {

    private static final String GOAL_ACCOUNT_CURRENCY = "INR";

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
        if (type == null) {
            throw new IllegalArgumentException("Account type is required");
        }

        if (type == AccountType.GOAL) {
            throw new IllegalArgumentException(
                    "Goal accounts can only be created through goal management"
            );
        }

        validateOpeningBalance(openingBalance);

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

    /**
     * Creates the dedicated zero-balance account belonging to a goal.
     * This method is intended for internal service use, not the Accounts API.
     */
    @Transactional
    public Account createGoalAccount(UUID userId, String goalName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account account = new Account(
                user,
                "Goal - " + goalName,
                AccountType.GOAL,
                GOAL_ACCOUNT_CURRENCY,
                new BigDecimal("0.00")
        );

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccounts(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return accountRepository.findByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID userId, UUID accountId) {
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
        Account account = getAccount(userId, accountId);

        if (account.getType() == AccountType.GOAL) {
            throw new IllegalArgumentException(
                    "Goal accounts cannot be updated through account management"
            );
        }

        if (type == AccountType.GOAL) {
            throw new IllegalArgumentException(
                    "An account cannot be converted into a goal account"
            );
        }

        account.setName(name);
        account.setType(type);
        account.setCurrency(currency);
        account.setUpdatedAt(java.time.OffsetDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(UUID userId, UUID accountId) {
        Account account = getAccount(userId, accountId);

        if (account.getType() == AccountType.GOAL) {
            throw new IllegalArgumentException(
                    "Goal accounts cannot be deleted through account management"
            );
        }

        boolean hasTransactions =
                transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        boolean hasInvestmentTransactions =
                investmentTransactionRepository.existsByAccount_Id(accountId);

        if (hasTransactions || hasInvestmentTransactions) {
            throw new IllegalArgumentException(
                    "Account cannot be deleted because it has financial transactions"
            );
        }

        accountRepository.delete(account);
    }

    @Transactional
    public void deleteGoalAccount(UUID userId, UUID accountId) {
        Account account = getAccount(userId, accountId);

        if (account.getType() != AccountType.GOAL) {
                throw new IllegalArgumentException(
                        "The account is not a goal account"
                );
        }

        boolean hasTransactions =
                transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        boolean hasInvestmentTransactions =
                investmentTransactionRepository.existsByAccount_Id(accountId);

        if (hasTransactions || hasInvestmentTransactions) {
                throw new IllegalArgumentException(
                        "Goal cannot be deleted because its account has financial transactions"
                );
        }

        accountRepository.delete(account);
    }

    private void validateOpeningBalance(BigDecimal openingBalance) {
        if (openingBalance == null) {
            throw new IllegalArgumentException(
                    "Opening balance is required"
            );
        }

        if (openingBalance.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places"
            );
        }
    }
}
