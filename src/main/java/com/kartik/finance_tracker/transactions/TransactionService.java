package com.kartik.finance_tracker.transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AccountBalanceService accountBalanceService;

    public TransactionService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            AccountBalanceService accountBalanceService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.accountBalanceService = accountBalanceService;
    }

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        Account destinationAccount = null;

        if (destinationAccountId != null) {
            destinationAccount = accountRepository.findById(destinationAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

            if (!destinationAccount.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException(
                        "Destination account does not belong to user"
                );
            }
        }

        Category category = null;

        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if (!category.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Category does not belong to user");
            }
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
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
                        "Transfer requires a destination account"
                );
            }

            if (category != null) {
                throw new IllegalArgumentException(
                        "Transfer cannot have a category"
                );
            }

            if (account.getId().equals(destinationAccount.getId())) {
                throw new IllegalArgumentException(
                        "Source and destination accounts must be different"
                );
            }

        } else {

            if (category == null) {
                throw new IllegalArgumentException(
                        "Category is required for income and expense"
                );
            }

            if (destinationAccount != null) {
                throw new IllegalArgumentException(
                        "Income and expense cannot have a destination account"
                );
            }

            if (type == TransactionType.INCOME
                    && category.getType() != CategoryType.INCOME) {
                throw new IllegalArgumentException(
                        "Income transaction requires an income category"
                );
            }

            if (type == TransactionType.EXPENSE
                    && category.getType() != CategoryType.EXPENSE) {
                throw new IllegalArgumentException(
                        "Expense transaction requires an expense category"
                );
            }
        }

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

        return transactionRepository.save(transaction);
    }
}