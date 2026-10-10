package com.kartik.finance_tracker.transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AccountBalanceService accountBalanceService;
    private final CacheManager cacheManager;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            AccountBalanceService accountBalanceService,
            CacheManager cacheManager
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.accountBalanceService = accountBalanceService;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public Transaction createTransaction(
            UUID userId,
            UUID accountId,
            UUID destinationAccountId,
            UUID categoryId,
            TransactionType type,
            BigDecimal amount,
            String description,
            OffsetDateTime occurredAt
    ) {
        return createTransactionInternal(
                userId,
                accountId,
                destinationAccountId,
                categoryId,
                type,
                amount,
                description,
                occurredAt,
                false
        );
    }

    /**
     * Creates a contribution transfer through the goal-management flow.
     * Generic transfers cannot use a Goal Account as their destination.
     */
    @Transactional
    public Transaction createGoalContribution(
            UUID userId,
            UUID sourceAccountId,
            UUID goalAccountId,
            BigDecimal amount,
            String description,
            OffsetDateTime occurredAt
    ) {
        return createTransactionInternal(
                userId,
                sourceAccountId,
                goalAccountId,
                null,
                TransactionType.TRANSFER,
                amount,
                description,
                occurredAt,
                true
        );
    }

    private Transaction createTransactionInternal(
            UUID userId,
            UUID accountId,
            UUID destinationAccountId,
            UUID categoryId,
            TransactionType type,
            BigDecimal amount,
            String description,
            OffsetDateTime occurredAt,
            boolean allowGoalDestination
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found");
        }

        Account destinationAccount = null;

        if (destinationAccountId != null) {
            destinationAccount = accountRepository.findById(destinationAccountId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Destination account not found"));

            if (!destinationAccount.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException(
                        "Destination account not found");
            }
        }

        if (destinationAccount != null) {
            if (destinationAccount.getType() == AccountType.GOAL
                    && !allowGoalDestination) {
                throw new IllegalArgumentException(
                        "Goal accounts can only receive contributions through goal management");
            }

            if (allowGoalDestination
                    && destinationAccount.getType() != AccountType.GOAL) {
                throw new IllegalArgumentException(
                        "Goal contribution destination must be a goal account");
            }
        } else if (allowGoalDestination) {
            throw new IllegalArgumentException(
                    "Goal contribution requires a goal account destination");
        }

        if (allowGoalDestination && account.getType() == AccountType.GOAL) {
            throw new IllegalArgumentException(
                    "A goal account cannot be the source of its own contribution");
        }

        Category category = null;

        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Category not found"));

            if (!category.getUser().getId().equals(userId)) {
                throw new ResourceNotFoundException("Category not found");
            }
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 2 decimal places");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        if (type == TransactionType.TRANSFER) {
            if (destinationAccount == null) {
                throw new IllegalArgumentException(
                        "Transfer requires a destination account");
            }

            if (category != null) {
                throw new IllegalArgumentException(
                        "Transfer cannot have a category");
            }

            if (account.getId().equals(destinationAccount.getId())) {
                throw new IllegalArgumentException(
                        "Source and destination accounts must be different");
            }

            if (!account.getCurrency().equals(destinationAccount.getCurrency())) {
                throw new IllegalArgumentException(
                        "Source and destination accounts must use the same currency");
            }

            if (account.getType() == AccountType.CREDIT_CARD) {
                throw new IllegalArgumentException(
                        "Credit card cannot be the source of a transfer");
            }
        } else {
            if (account.getType() == AccountType.GOAL) {
                throw new IllegalArgumentException(
                        "Goal accounts only support transfers");
            }

            if (category == null) {
                throw new IllegalArgumentException(
                        "Category is required for income and expense");
            }

            if (!category.getChildren().isEmpty()) {
                throw new IllegalArgumentException(
                        "Parent categories cannot be used for transactions");
            }

            if (destinationAccount != null) {
                throw new IllegalArgumentException(
                        "Income and expense cannot have a destination account");
            }

            if (type == TransactionType.INCOME
                    && category.getType() != CategoryType.INCOME) {
                throw new IllegalArgumentException(
                        "Income transaction requires an income category");
            }

            if (type == TransactionType.EXPENSE
                    && category.getType() != CategoryType.EXPENSE) {
                throw new IllegalArgumentException(
                        "Expense transaction requires an expense category");
            }
        }

        validateBalance(account, destinationAccount, type, amount);

        Transaction transaction = new Transaction(
                user,
                account,
                destinationAccount,
                category,
                type,
                amount,
                description,
                occurredAt
        );

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        evictMonthlySummaryCache(userId, occurredAt);

        return savedTransaction;
    }

    @Transactional
    public Transaction voidTransaction(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found");
        }

        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            throw new IllegalArgumentException(
                    "Transaction is already voided");
        }

        transaction.setStatus(TransactionStatus.VOIDED);
        transaction.setUpdatedAt(OffsetDateTime.now());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        evictMonthlySummaryCache(userId, transaction.getOccurredAt());

        return savedTransaction;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return transactionRepository.findAllByUser_Id(userId);
    }

    private void evictMonthlySummaryCache(
            UUID userId,
            OffsetDateTime occurredAt
    ) {
        Cache cache = cacheManager.getCache("monthlySummary");

        if (cache != null) {
            String cacheKey = userId
                    + ":"
                    + occurredAt.getYear()
                    + ":"
                    + occurredAt.getMonthValue();

            cache.evictIfPresent(cacheKey);
        }
    }

    private void validateBalance(
            Account account,
            Account destinationAccount,
            TransactionType type,
            BigDecimal amount
    ) {
        if (type == TransactionType.INCOME) {
            return;
        }

        BigDecimal currentBalance =
                accountBalanceService.calculateBalance(account.getId());

        if (type == TransactionType.EXPENSE) {
            if (account.getType() == AccountType.CREDIT_CARD) {
                return;
            }

            if (amount.compareTo(currentBalance) > 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }

            return;
        }

        if (type == TransactionType.TRANSFER) {
            if (amount.compareTo(currentBalance) > 0) {
                throw new IllegalArgumentException("Insufficient funds");
            }

            if (destinationAccount.getType() == AccountType.CREDIT_CARD) {
                BigDecimal currentCardBalance =
                        accountBalanceService.calculateBalance(
                                destinationAccount.getId());

                if (amount.compareTo(currentCardBalance) > 0) {
                    throw new IllegalArgumentException(
                            "Credit card payment exceeds amount owed");
                }
            }
        }
    }
}
