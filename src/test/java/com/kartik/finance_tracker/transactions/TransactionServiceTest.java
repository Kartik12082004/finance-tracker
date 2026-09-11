package com.kartik.finance_tracker.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;
    private AccountBalanceService accountBalanceService;

    private TransactionService transactionService;

    private User user;
    private Account bankAccount;
    private Account secondBankAccount;

    @BeforeEach
    void setUp() {

        transactionRepository = mock(TransactionRepository.class);
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        accountBalanceService = mock(AccountBalanceService.class);

        transactionService = new TransactionService(
                transactionRepository,
                userRepository,
                accountRepository,
                categoryRepository,
                accountBalanceService
        );

        user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        bankAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        secondBankAccount = new Account(
                user,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );
    }

    @Test
    void createTransaction_shouldCreateExpense() {

        Category category = category(
                "Food",
                CategoryType.EXPENSE
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        OffsetDateTime occurredAt = OffsetDateTime.now();

        Transaction result = transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                category.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Groceries",
                occurredAt
        );

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getAccount()).isSameAs(bankAccount);
        assertThat(result.getCategory()).isSameAs(category);
        assertThat(result.getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(result.getAmount()).isEqualByComparingTo("500.00");
        assertThat(result.getDescription()).isEqualTo("Groceries");
        assertThat(result.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(result.getDestinationAccount()).isNull();

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldCreateIncome() {

        Category category = category(
                "Salary",
                CategoryType.INCOME
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                category.getId(),
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                "September Salary",
                OffsetDateTime.now()
        );

        assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.getCategory()).isSameAs(category);
        assertThat(result.getDestinationAccount()).isNull();

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldCreateTransfer() {

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("20000.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                secondBankAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("10000.00"),
                "Transfer to SBI",
                OffsetDateTime.now()
        );

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getAccount()).isSameAs(bankAccount);
        assertThat(result.getDestinationAccount())
                .isSameAs(secondBankAccount);
        assertThat(result.getCategory()).isNull();
        assertThat(result.getAmount()).isEqualByComparingTo("10000.00");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenAmountIsZero() {

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                create(
                        null,
                        TransactionType.EXPENSE,
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenAmountIsNegative() {

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                create(
                        null,
                        TransactionType.EXPENSE,
                        new BigDecimal("-500.00")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenTypeIsNull() {

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                create(
                        null,
                        null,
                        new BigDecimal("500.00")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction type is required");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenCategoryIsMissing() {

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                create(
                        null,
                        TransactionType.EXPENSE,
                        new BigDecimal("500.00")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category is required for income and expense");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenCategoryTypeDoesNotMatchTransactionType() {

        Category incomeCategory = category(
                "Salary",
                CategoryType.INCOME
        );

        stubUserAndAccount(bankAccount);
        stubCategory(incomeCategory);

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        null,
                        incomeCategory.getId(),
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

        Category category = category(
                "Food",
                CategoryType.EXPENSE
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        secondBankAccount.getId(),
                        category.getId(),
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

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                create(
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("10000.00")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer requires a destination account");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenSourceAndDestinationAreSame() {

        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        bankAccount.getId(),
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

    private Transaction create(
            java.util.UUID categoryId,
            TransactionType type,
            BigDecimal amount
    ) {
        return transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                categoryId,
                type,
                amount,
                "Test transaction",
                OffsetDateTime.now()
        );
    }

    private Category category(
            String name,
            CategoryType type
    ) {
        return new Category(
                user,
                name,
                type,
                null,
                true
        );
    }

    private void stubUserAndAccount(Account account) {

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(accountRepository.findById(account.getId()))
                .thenReturn(Optional.of(account));
    }

    private void stubCategory(Category category) {

        when(categoryRepository.findById(category.getId()))
                .thenReturn(Optional.of(category));
    }
}