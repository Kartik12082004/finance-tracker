package com.kartik.finance_tracker.transactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

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
    private CacheManager cacheManager;

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
        cacheManager = mock(CacheManager.class);

        transactionService = new TransactionService(
                transactionRepository,
                userRepository,
                accountRepository,
                categoryRepository,
                accountBalanceService,
                cacheManager
        );

        // All tests use one user so ownership rules can be tested consistently.
        user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        // Create two accounts for testing transfers between accounts.
        bankAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                BigDecimal.ZERO
        );

        secondBankAccount = new Account(
                user,
                "SBI Savings",
                AccountType.BANK,
                "INR",
                BigDecimal.ZERO
        );
    }

    @Test
    void createTransaction_shouldCreateExpense() {

        // Expenses require an EXPENSE category and reduce the source account balance.
        Category category = category(
                "Food",
                CategoryType.EXPENSE
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // The account has enough money for this expense.
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

        // A valid transaction should be persisted.
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldCreateIncome() {

        // Income requires an INCOME category and increases the source account balance.
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

        // Transfers move money between accounts and therefore have no category.
        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        // The source account must have enough money for the transfer.
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
    void createTransaction_shouldAllowCreditCardPaymentWithinAmountOwed() {

        Account creditCard = new Account(
                user,
                "HDFC Credit Card",
                AccountType.CREDIT_CARD,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(creditCard.getId()))
                .thenReturn(Optional.of(creditCard));

        // The bank account has enough money to make the payment.
        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        // The credit card currently has 5,000 of outstanding debt.
        when(accountBalanceService.calculateBalance(creditCard.getId()))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Paying 3,000 is valid because it does not exceed the 5,000 owed.
        Transaction result = transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                creditCard.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("3000.00"),
                "Credit card payment",
                OffsetDateTime.now()
        );

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getAccount()).isSameAs(bankAccount);
        assertThat(result.getDestinationAccount()).isSameAs(creditCard);
        assertThat(result.getAmount()).isEqualByComparingTo("3000.00");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectCreditCardPaymentExceedingAmountOwed() {

        Account creditCard = new Account(
                user,
                "HDFC Credit Card",
                AccountType.CREDIT_CARD,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(creditCard.getId()))
                .thenReturn(Optional.of(creditCard));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        // Only 5,000 is currently owed on the credit card.
        when(accountBalanceService.calculateBalance(creditCard.getId()))
                .thenReturn(new BigDecimal("5000.00"));

        // Phase 1 rejects payments that exceed the amount currently owed.
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        creditCard.getId(),
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("6000.00"),
                        "Credit card payment",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit card payment exceeds amount owed");

        // Invalid transactions must never be persisted.
        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectExpenseWhenBalanceIsInsufficient() {

        Category category = category(
                "Food",
                CategoryType.EXPENSE
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        // Bank accounts cannot go below zero through an expense.
        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("1000.00"));

        // The requested expense is greater than the available balance.
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        null,
                        category.getId(),
                        TransactionType.EXPENSE,
                        new BigDecimal("1500.00"),
                        "Groceries",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectTransferWhenBalanceIsInsufficient() {

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        // Bank accounts cannot transfer out more money than they currently hold.
        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("1000.00"));

        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        secondBankAccount.getId(),
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("1500.00"),
                        "Transfer",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectCreditCardAsTransferSource() {

        Account creditCard = new Account(
                user,
                "HDFC Credit Card",
                AccountType.CREDIT_CARD,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(creditCard);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        // Phase 1 does not support credit-card cash advances.
        // Therefore, a credit card cannot be the source of a transfer.
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        creditCard.getId(),
                        secondBankAccount.getId(),
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("1000.00"),
                        "Cash advance",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit card cannot be the source of a transfer");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectTransferWithDifferentCurrencies() {

        Account usdAccount = new Account(
                user,
                "USD Account",
                AccountType.BANK,
                "USD",
                BigDecimal.ZERO
        );

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(usdAccount.getId()))
                .thenReturn(Optional.of(usdAccount));

        // Phase 1 transfers do not support currency conversion.
        // Source and destination accounts must therefore use the same currency.
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        user.getId(),
                        bankAccount.getId(),
                        usdAccount.getId(),
                        null,
                        TransactionType.TRANSFER,
                        new BigDecimal("1000.00"),
                        "Currency mismatch",
                        OffsetDateTime.now()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source and destination accounts must use the same currency");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectAmountWithMoreThanTwoDecimalPlaces() {

        stubUserAndAccount(bankAccount);

        // Amounts are stored with a maximum precision of two decimal places.
        // More precise values must be rejected instead of silently rounded.
        assertThatThrownBy(() ->
                create(
                        null,
                        TransactionType.EXPENSE,
                        new BigDecimal("500.123")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot have more than 2 decimal places");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldThrowWhenAmountIsZero() {

        stubUserAndAccount(bankAccount);

        // A transaction must always represent a positive amount.
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

        // Negative transaction amounts are not allowed.
        // Direction is represented by the transaction type instead.
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

        // Every transaction must explicitly declare whether it is income,
        // expense, or transfer.
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

        // Income and expense transactions must have a category.
        // Transfers are the only transaction type that does not use one.
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

        // An EXPENSE transaction cannot use an INCOME category.
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

        // Transfers are account-to-account movements, so they do not belong
        // to an income or expense category.
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

        // A transfer must specify the account receiving the money.
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

        // A transfer between the same account has no meaningful financial effect
        // and is therefore rejected.
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

    @Test
    void createTransaction_shouldDefaultToPostedStatus() {

        Category category = category(
                "Food",
                CategoryType.EXPENSE
        );

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = create(
                category.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("500.00")
        );

        assertThat(result.getStatus())
                .isEqualTo(TransactionStatus.POSTED);
    }

    @Test
    void voidTransaction_shouldMarkPostedTransactionAsVoided() {

        Transaction transaction = new Transaction(
                user,
                bankAccount,
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Groceries",
                OffsetDateTime.parse("2026-09-13T10:00:00+05:30")
        );

        UUID transactionId = transaction.getId();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = transactionService.voidTransaction(
                user.getId(),
                transactionId
        );

        assertThat(result.getStatus())
                .isEqualTo(TransactionStatus.VOIDED);

        verify(transactionRepository).save(transaction);
        verify(cacheManager).getCache("monthlySummary");
    }

    @Test
    void voidTransaction_shouldRejectAlreadyVoidedTransaction() {

        Transaction transaction = new Transaction(
                user,
                bankAccount,
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Groceries",
                OffsetDateTime.parse("2026-09-13T10:00:00+05:30")
        );

        transaction.setStatus(TransactionStatus.VOIDED);

        when(transactionRepository.findById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        assertThatThrownBy(() ->
                transactionService.voidTransaction(
                        user.getId(),
                        transaction.getId()
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction is already voided");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void voidTransaction_shouldRejectTransactionOwnedByAnotherUser() {

        User otherUser = new User(
                "other@example.com",
                "hashed-password",
                "Other"
        );

        Account otherAccount = new Account(
                otherUser,
                "Other Bank",
                AccountType.BANK,
                "INR",
                BigDecimal.ZERO
        );

        Transaction transaction = new Transaction(
                otherUser,
                otherAccount,
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Other user's transaction",
                OffsetDateTime.parse("2026-09-13T10:00:00+05:30")
        );

        when(transactionRepository.findById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        assertThatThrownBy(() ->
                transactionService.voidTransaction(
                        user.getId(),
                        transaction.getId()
                )
        )
                .isInstanceOf(
                        com.kartik.finance_tracker.common.exception.ResourceNotFoundException.class
                )
                .hasMessage("Transaction not found");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void voidTransaction_shouldThrowWhenTransactionDoesNotExist() {

        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transactionService.voidTransaction(
                        user.getId(),
                        transactionId
                )
        )
                .isInstanceOf(
                        com.kartik.finance_tracker.common.exception.ResourceNotFoundException.class
                )
                .hasMessage("Transaction not found");

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    private Transaction create(
            UUID categoryId,
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
