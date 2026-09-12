package com.kartik.finance_tracker.transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
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

        // Verify that the transaction belongs to an existing user.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Load and verify ownership of the account where the transaction originates.
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        Account destinationAccount = null;

        if (destinationAccountId != null) {

            // Transfers have a second account that receives the money.
            destinationAccount = accountRepository.findById(destinationAccountId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Destination account not found"));

            if (!destinationAccount.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException(
                        "Destination account does not belong to user"
                );
            }
        }

        Category category = null;

        if (categoryId != null) {

            // Income and expense transactions use categories for classification.
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            if (!category.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Category does not belong to user");
            }
        }

        // Transaction amounts must be positive and fit the database precision.
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (amount.scale() > 4) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than 4 decimal places"
            );
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        if (type == TransactionType.TRANSFER) {

            /*
             * Transfers move money between two accounts.
             * They therefore cannot have a category.
             */
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

            // Phase 1 does not support foreign-exchange transfers.
            if (!account.getCurrency().equals(destinationAccount.getCurrency())) {
                throw new IllegalArgumentException(
                        "Source and destination accounts must use the same currency"
                );
            }

            /*
             * Credit-card-to-account transfers would represent a cash advance,
             * which is outside the transaction model supported in Phase 1.
             */
            if (account.getType() == AccountType.CREDIT_CARD) {
                throw new IllegalArgumentException(
                        "Credit card cannot be the source of a transfer"
                );
            }

        } else {

            /*
             * Income and expense transactions represent money entering or
             * leaving an account, so they require a matching category.
             */
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

        // Validate that the transaction is allowed based on the current balance.
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

        return transactionRepository.save(transaction);
    }

    private void validateBalance(
            Account account,
            Account destinationAccount,
            TransactionType type,
            BigDecimal amount
    ) {

        // Income increases the balance, so it does not require a funds check.
        if (type == TransactionType.INCOME) {
            return;
        }

        BigDecimal currentBalance =
                accountBalanceService.calculateBalance(account.getId());

        if (type == TransactionType.EXPENSE) {

            /*
             * Credit-card balances represent debt:
             *  0      = nothing owed
             *  +5000  = 5000 owed
             *
             * Therefore, credit-card spending is allowed even when
             * the current balance is zero.
             */
            if (account.getType() == AccountType.CREDIT_CARD) {
                return;
            }

            // Normal asset accounts cannot be allowed to go below zero.
            if (amount.compareTo(currentBalance) > 0) {
                throw new IllegalArgumentException(
                        "Insufficient funds"
                );
            }

            return;
        }

        if (type == TransactionType.TRANSFER) {

            // The source account must have enough money for the transfer.
            if (amount.compareTo(currentBalance) > 0) {
                throw new IllegalArgumentException(
                        "Insufficient funds"
                );
            }

            if (destinationAccount.getType() == AccountType.CREDIT_CARD) {

                /*
                 * Paying a credit card reduces its outstanding debt.
                 * We do not allow payments larger than the amount currently owed.
                 */
                BigDecimal currentCardBalance =
                        accountBalanceService.calculateBalance(
                                destinationAccount.getId()
                        );

                if (amount.compareTo(currentCardBalance) > 0) {
                    throw new IllegalArgumentException(
                            "Credit card payment exceeds amount owed"
                    );
                }
            }
        }
    }
}