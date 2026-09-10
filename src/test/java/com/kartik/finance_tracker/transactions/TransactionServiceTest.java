package com.kartik.finance_tracker.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

public class TransactionServiceTest {

    @Test
    void createTransaction_shouldCreateExpense() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();
        UUID categoryId = category.getId();

        OffsetDateTime occurredAt = OffsetDateTime.now();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        Transaction savedTransaction = new Transaction(
                user,
                account,
                null,
                category,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Groceries",
                occurredAt
        );

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        Transaction result = transactionService.createTransaction(
                userId,
                accountId,
                null,
                categoryId,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Groceries",
                occurredAt
        );

        assertThat(result).isSameAs(savedTransaction);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction transaction =
                transactionCaptor.getValue();

        assertThat(transaction.getUser()).isSameAs(user);
        assertThat(transaction.getAccount()).isSameAs(account);
        assertThat(transaction.getCategory()).isSameAs(category);
        assertThat(transaction.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(transaction.getAmount())
                .isEqualByComparingTo("500.00");
        assertThat(transaction.getDescription())
                .isEqualTo("Groceries");
        assertThat(transaction.getOccurredAt())
                .isEqualTo(occurredAt);
        assertThat(transaction.getDestinationAccount())
                .isNull();
    }


    @Test
    void createTransaction_shouldCreateIncome() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Category category = new Category(
                user,
                "Salary",
                CategoryType.INCOME,
                null,
                true
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();
        UUID categoryId = category.getId();

        OffsetDateTime occurredAt = OffsetDateTime.now();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        Transaction savedTransaction = new Transaction(
                user,
                account,
                null,
                category,
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                "September Salary",
                occurredAt
        );

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        Transaction result = transactionService.createTransaction(
                userId,
                accountId,
                null,
                categoryId,
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                "September Salary",
                occurredAt
        );

        assertThat(result).isSameAs(savedTransaction);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction transaction =
                transactionCaptor.getValue();

        assertThat(transaction.getType())
                .isEqualTo(TransactionType.INCOME);
        assertThat(transaction.getAmount())
                .isEqualByComparingTo("50000.00");
        assertThat(transaction.getCategory())
                .isSameAs(category);
        assertThat(transaction.getDestinationAccount())
                .isNull();
    }


    @Test
    void createTransaction_shouldCreateTransfer() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account sourceAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Account destinationAccount = new Account(
                user,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();

        OffsetDateTime occurredAt = OffsetDateTime.now();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccountId))
                .thenReturn(Optional.of(destinationAccount));

        Transaction savedTransaction = new Transaction(
                user,
                sourceAccount,
                destinationAccount,
                null,
                TransactionType.TRANSFER,
                new BigDecimal("10000.00"),
                "Transfer to SBI",
                occurredAt
        );

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(savedTransaction);

        Transaction result = transactionService.createTransaction(
                userId,
                sourceAccountId,
                destinationAccountId,
                null,
                TransactionType.TRANSFER,
                new BigDecimal("10000.00"),
                "Transfer to SBI",
                occurredAt
        );

        assertThat(result).isSameAs(savedTransaction);

        ArgumentCaptor<Transaction> transactionCaptor =
                ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction transaction =
                transactionCaptor.getValue();

        assertThat(transaction.getType())
                .isEqualTo(TransactionType.TRANSFER);
        assertThat(transaction.getAccount())
                .isSameAs(sourceAccount);
        assertThat(transaction.getDestinationAccount())
                .isSameAs(destinationAccount);
        assertThat(transaction.getCategory())
                .isNull();
        assertThat(transaction.getAmount())
                .isEqualByComparingTo("10000.00");
    }


    @Test
    void createTransaction_shouldThrowWhenAmountIsZero() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        null,
                        TransactionType.EXPENSE,
                        BigDecimal.ZERO,
                        "Invalid expense",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenAmountIsNegative() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        null,
                        TransactionType.EXPENSE,
                        new BigDecimal("-500.00"),
                        "Invalid expense",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenTypeIsNull() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        null,
                        null,
                        new BigDecimal("500.00"),
                        "Invalid transaction",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction type is required");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenCategoryIsMissing() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        null,
                        TransactionType.EXPENSE,
                        new BigDecimal("500.00"),
                        "Groceries",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category is required for income and expense");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenCategoryTypeDoesNotMatchTransactionType() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Category incomeCategory = new Category(
                user,
                "Salary",
                CategoryType.INCOME,
                null,
                true
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();
        UUID categoryId = incomeCategory.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(incomeCategory));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("500.00"),
                        "Invalid category",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expense transaction requires an expense category");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenTransferHasCategory() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account sourceAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Account destinationAccount = new Account(
                user,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        UUID userId = user.getId();
        UUID sourceAccountId = sourceAccount.getId();
        UUID destinationAccountId = destinationAccount.getId();
        UUID categoryId = category.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(sourceAccountId))
                .thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(destinationAccountId))
                .thenReturn(Optional.of(destinationAccount));
        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        sourceAccountId,
                        destinationAccountId,
                        categoryId,
                        TransactionType.TRANSFER,
                        new BigDecimal("10000.00"),
                        "Invalid transfer",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer cannot have a category");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenTransferHasNoDestination() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        null,
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("10000.00"),
                        "Invalid transfer",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer requires a destination account");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }


    @Test
    void createTransaction_shouldThrowWhenSourceAndDestinationAreSame() {
        TransactionRepository transactionRepository =
                mock(TransactionRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        AccountRepository accountRepository =
                mock(AccountRepository.class);
        CategoryRepository categoryRepository =
                mock(CategoryRepository.class);

        TransactionService transactionService =
                new TransactionService(
                        transactionRepository,
                        userRepository,
                        accountRepository,
                        categoryRepository
                );

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        UUID userId = user.getId();
        UUID accountId = account.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        userId,
                        accountId,
                        accountId,
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("10000.00"),
                        "Invalid transfer",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source and destination accounts must be different");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }
}