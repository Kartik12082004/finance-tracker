package com.kartik.finance_tracker.accounts;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.Goal;
import com.kartik.finance_tracker.goals.GoalRepository;
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
    private final GoalRepository goalRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            InvestmentTransactionRepository investmentTransactionRepository,
            GoalRepository goalRepository
    ) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.investmentTransactionRepository = investmentTransactionRepository;
        this.goalRepository = goalRepository;
    }

    @Transactional
    public Account createAccount(
            UUID userId,
            String name,
            AccountType type,
            String currency,
            BigDecimal openingBalance,
            BigDecimal creditLimit,
            UUID parentAccountId
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

        Account parentAccount = validateCreditCardDetails(
                userId, type, creditLimit, parentAccountId
        );

        Account account = new Account(
                user,
                name,
                type,
                currency,
                openingBalance,
                creditLimit,
                parentAccount
        );

        return accountRepository.save(account);
    }

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

        List<UUID> archivedGoalAccountIds = goalRepository
                .findByUser_IdAndArchivedTrue(userId)
                .stream()
                .map(Goal::getAccount)
                .filter(java.util.Objects::nonNull)
                .map(Account::getId)
                .toList();

        return accountRepository.findByUser_Id(userId)
                .stream()
                .filter(account -> !archivedGoalAccountIds.contains(account.getId()))
                .toList();
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
            String currency,
            BigDecimal creditLimit,
            UUID parentAccountId
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

        validateOpeningBalance(account.getOpeningBalance());

        Account parentAccount = validateCreditCardDetails(
                userId, type, creditLimit, parentAccountId
        );

        if (account.getType() != type
                && (transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                        accountId, accountId)
                    || investmentTransactionRepository.existsByAccount_Id(accountId))) {
            throw new IllegalArgumentException(
                    "Account type cannot be changed after financial transactions exist"
            );
        }

        account.setName(name);
        account.setType(type);
        account.setCurrency(currency);
        account.setCreditLimit(creditLimit);
        account.setParentAccount(parentAccount);
        account.setUpdatedAt(OffsetDateTime.now());

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
                        accountId, accountId
                );

        boolean hasInvestmentTransactions =
                investmentTransactionRepository.existsByAccount_Id(accountId);

        if (hasTransactions || hasInvestmentTransactions) {
            throw new IllegalArgumentException(
                    "Account cannot be deleted because it has financial transactions"
            );
        }

        if (accountRepository.existsByParentAccount_Id(accountId)) {
            throw new IllegalArgumentException(
                    "Account cannot be deleted while credit cards are linked to it"
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
                        accountId, accountId
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

    private Account validateCreditCardDetails(
            UUID userId,
            AccountType type,
            BigDecimal creditLimit,
            UUID parentAccountId
    ) {
        if (type != AccountType.CREDIT_CARD) {
            if (creditLimit != null || parentAccountId != null) {
                throw new IllegalArgumentException(
                        "Credit limit and parent account are only allowed for credit cards"
                );
            }
            return null;
        }

        if (creditLimit == null || creditLimit.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Credit limit must be greater than zero"
            );
        }

        if (creditLimit.scale() > 2) {
            throw new IllegalArgumentException(
                    "Credit limit cannot have more than 2 decimal places"
            );
        }

        if (parentAccountId == null) {
            throw new IllegalArgumentException(
                    "Credit card must be linked to a bank account"
            );
        }

        Account parentAccount = accountRepository
                .findByIdAndUser_Id(parentAccountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Linked bank account not found"
                        ));

        if (parentAccount.getType() != AccountType.BANK) {
            throw new IllegalArgumentException(
                    "Credit card must be linked to a bank account"
            );
        }

        return parentAccount;
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
