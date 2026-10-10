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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;
    private AccountBalanceService accountBalanceService;
    private CacheManager cacheManager;
    private Cache monthlySummaryCache;

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
        monthlySummaryCache = mock(Cache.class);

        transactionService = new TransactionService(
                transactionRepository,
                userRepository,
                accountRepository,
                categoryRepository,
                accountBalanceService,
                cacheManager
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

        when(cacheManager.getCache("monthlySummary"))
                .thenReturn(monthlySummaryCache);

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTransaction_shouldCreateExpense() {
        Category category = category("Food", CategoryType.EXPENSE);

        stubUserAndAccount(bankAccount);
        stubCategory(category);

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
        verify(monthlySummaryCache).evictIfPresent(anyString());
    }

    @Test
    void createTransaction_shouldCreateIncome() {
        Category category = category("Salary", CategoryType.INCOME);

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        Transaction result = transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                category.getId(),
                TransactionType.INCOME,
                new BigDecimal("50000.00"),
                "Monthly salary",
                OffsetDateTime.now()
        );

        assertThat(result.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(result.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(result.getCategory()).isSameAs(category);

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldCreateTransfer() {
        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("20000.00"));

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
        assertThat(result.getDestinationAccount()).isSameAs(secondBankAccount);
        assertThat(result.getCategory()).isNull();
        assertThat(result.getAmount()).isEqualByComparingTo("10000.00");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectExpenseWhenBalanceIsInsufficient() {
        Category category = category("Food", CategoryType.EXPENSE);

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                category.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("1500.00"),
                "Groceries",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectTransferWhenBalanceIsInsufficient() {
        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                secondBankAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1500.00"),
                "Transfer",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient funds");

        verify(transactionRepository, never()).save(any(Transaction.class));
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

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                creditCard.getId(),
                secondBankAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Cash advance",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credit card cannot be the source of a transfer");

        verify(transactionRepository, never()).save(any(Transaction.class));
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

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                usdAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Currency mismatch",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Source and destination accounts must use the same currency");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("500.123"),
                "Invalid amount",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot have more than 2 decimal places");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectZeroAmount() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                null,
                TransactionType.EXPENSE,
                BigDecimal.ZERO,
                "Invalid amount",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectNegativeAmount() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("-500.00"),
                "Invalid amount",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectMissingCategory() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                null,
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Missing category",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category is required for income and expense");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectCategoryTypeMismatch() {
        Category incomeCategory = category("Salary", CategoryType.INCOME);

        stubUserAndAccount(bankAccount);
        stubCategory(incomeCategory);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                incomeCategory.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("500.00"),
                "Invalid category",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expense transaction requires an expense category");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectTransferWithCategory() {
        Category category = category("Food", CategoryType.EXPENSE);

        stubUserAndAccount(bankAccount);
        stubCategory(category);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                secondBankAccount.getId(),
                category.getId(),
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Invalid transfer",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer cannot have a category");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectTransferWithoutDestination() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                null,
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Missing destination",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer requires a destination account");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectSameSourceAndDestination() {
        stubUserAndAccount(bankAccount);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                bankAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Invalid transfer",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source and destination accounts must be different");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectIncomeOnGoalAccount() {
        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        Category category = category("Salary", CategoryType.INCOME);

        stubUserAndAccount(goalAccount);
        stubCategory(category);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                goalAccount.getId(),
                null,
                category.getId(),
                TransactionType.INCOME,
                new BigDecimal("1000.00"),
                "Invalid goal income",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Goal accounts only support transfers");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectExpenseOnGoalAccount() {
        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        Category category = category("Food", CategoryType.EXPENSE);

        stubUserAndAccount(goalAccount);
        stubCategory(category);

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                goalAccount.getId(),
                null,
                category.getId(),
                TransactionType.EXPENSE,
                new BigDecimal("100.00"),
                "Invalid goal expense",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Goal accounts only support transfers");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectGenericTransferIntoGoalAccount() {
        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(goalAccount.getId()))
                .thenReturn(Optional.of(goalAccount));

        assertThatThrownBy(() -> transactionService.createTransaction(
                user.getId(),
                bankAccount.getId(),
                goalAccount.getId(),
                null,
                TransactionType.TRANSFER,
                new BigDecimal("1000.00"),
                "Generic transfer to goal",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Goal accounts can only receive contributions through goal management");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createGoalContribution_shouldCreateTransferIntoGoalAccount() {
        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(goalAccount.getId()))
                .thenReturn(Optional.of(goalAccount));

        when(accountBalanceService.calculateBalance(bankAccount.getId()))
                .thenReturn(new BigDecimal("10000.00"));

        Transaction result = transactionService.createGoalContribution(
                user.getId(),
                bankAccount.getId(),
                goalAccount.getId(),
                new BigDecimal("5000.00"),
                "Contribution to goal: Gaming PC",
                OffsetDateTime.now()
        );

        assertThat(result.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.getAccount()).isSameAs(bankAccount);
        assertThat(result.getDestinationAccount()).isSameAs(goalAccount);
        assertThat(result.getCategory()).isNull();
        assertThat(result.getAmount()).isEqualByComparingTo("5000.00");

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createGoalContribution_shouldRejectNonGoalDestination() {
        stubUserAndAccount(bankAccount);

        when(accountRepository.findById(secondBankAccount.getId()))
                .thenReturn(Optional.of(secondBankAccount));

        assertThatThrownBy(() -> transactionService.createGoalContribution(
                user.getId(),
                bankAccount.getId(),
                secondBankAccount.getId(),
                new BigDecimal("1000.00"),
                "Invalid goal contribution",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Goal contribution destination must be a goal account");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createGoalContribution_shouldRejectGoalAccountAsSource() {
        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        Account secondGoalAccount = new Account(
                user,
                "Goal - Laptop",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        stubUserAndAccount(goalAccount);

        when(accountRepository.findById(secondGoalAccount.getId()))
                .thenReturn(Optional.of(secondGoalAccount));

        assertThatThrownBy(() -> transactionService.createGoalContribution(
                user.getId(),
                goalAccount.getId(),
                secondGoalAccount.getId(),
                new BigDecimal("1000.00"),
                "Invalid goal contribution",
                OffsetDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A goal account cannot be the source of its own contribution");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void voidTransaction_shouldMarkTransactionAsVoided() {
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

        when(transactionRepository.findById(transaction.getId()))
                .thenReturn(Optional.of(transaction));

        Transaction result = transactionService.voidTransaction(
                user.getId(),
                transaction.getId()
        );

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.VOIDED);

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

        assertThatThrownBy(() -> transactionService.voidTransaction(
                user.getId(),
                transaction.getId()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction is already voided");

        verify(transactionRepository, never()).save(any(Transaction.class));
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

        assertThatThrownBy(() -> transactionService.voidTransaction(
                user.getId(),
                transaction.getId()
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void voidTransaction_shouldRejectMissingTransaction() {
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.voidTransaction(
                user.getId(),
                transactionId
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private Category category(String name, CategoryType type) {
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
