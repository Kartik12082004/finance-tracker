package com.kartik.finance_tracker.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.analytics.dto.AccountBalanceResponse;
import com.kartik.finance_tracker.analytics.dto.IncomeVsExpenseResponse;
import com.kartik.finance_tracker.analytics.dto.MonthlySummaryResponse;
import com.kartik.finance_tracker.analytics.dto.SpendingByCategoryResponse;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.UserRepository;

class AnalyticsServiceTest {

    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private AccountBalanceService accountBalanceService;

    private AnalyticsService analyticsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        accountBalanceService = mock(AccountBalanceService.class);

        analyticsService = new AnalyticsService(
                transactionRepository,
                userRepository,
                accountRepository,
                accountBalanceService
        );

        userId = UUID.randomUUID();
    }

    @Test
    void getMonthlySummary_shouldCalculateIncomeExpensesAndSavings() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // The repository returns database-level aggregates:
        // total income = 75,000 and total expenses = 42,000.
        when(transactionRepository.findMonthlyIncomeAndExpenses(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new Object[] {
                new Object[] {
                        new BigDecimal("75000.00"),
                        new BigDecimal("42000.00")
                }
        });

        MonthlySummaryResponse result =
                analyticsService.getMonthlySummary(userId, 2026, 9);

        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.month()).isEqualTo(9);
        assertThat(result.totalIncome()).isEqualByComparingTo("75000.00");
        assertThat(result.totalExpenses()).isEqualByComparingTo("42000.00");
        assertThat(result.netSavings()).isEqualByComparingTo("33000.00");

        // Savings rate = net savings / income × 100.
        assertThat(result.savingsRate()).isEqualByComparingTo("44.00");
    }

    @Test
    void getMonthlySummary_shouldReturnZeroSavingsRateWhenIncomeIsZero() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // A month with expenses but no income must not cause division by zero.
        when(transactionRepository.findMonthlyIncomeAndExpenses(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new Object[] {
                new Object[] {
                        BigDecimal.ZERO,
                        new BigDecimal("5000.00")
                }
        });

        MonthlySummaryResponse result =
                analyticsService.getMonthlySummary(userId, 2026, 9);

        assertThat(result.totalIncome())
                .isEqualByComparingTo("0.00");

        assertThat(result.totalExpenses())
                .isEqualByComparingTo("5000.00");

        assertThat(result.netSavings())
                .isEqualByComparingTo("-5000.00");

        assertThat(result.savingsRate())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void getMonthlySummary_shouldHandleMonthWithNoTransactions() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // PostgreSQL COALESCE converts an empty aggregate result to zero.
        when(transactionRepository.findMonthlyIncomeAndExpenses(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new Object[] {
                new Object[] {
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                }
        });

        MonthlySummaryResponse result =
                analyticsService.getMonthlySummary(userId, 2026, 9);

        assertThat(result.totalIncome())
                .isEqualByComparingTo("0.00");

        assertThat(result.totalExpenses())
                .isEqualByComparingTo("0.00");

        assertThat(result.netSavings())
                .isEqualByComparingTo("0.00");

        assertThat(result.savingsRate())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void getMonthlySummary_shouldRejectInvalidMonth() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                analyticsService.getMonthlySummary(userId, 2026, 13)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid year or month");

        // Invalid input must be rejected before querying the transaction repository.
        verify(transactionRepository, never())
                .findMonthlyIncomeAndExpenses(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getMonthlySummary_shouldRejectInvalidMonthZero() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                analyticsService.getMonthlySummary(userId, 2026, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid year or month");

        verify(transactionRepository, never())
                .findMonthlyIncomeAndExpenses(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getMonthlySummary_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThatThrownBy(() ->
                analyticsService.getMonthlySummary(userId, 2026, 9)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        // Analytics must never query transactions outside a valid user scope.
        verify(transactionRepository, never())
                .findMonthlyIncomeAndExpenses(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getMonthlySummary_shouldUseCorrectMonthlyDateRange() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(transactionRepository.findMonthlyIncomeAndExpenses(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(new Object[] {
                new Object[] {
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                }
        });

        analyticsService.getMonthlySummary(userId, 2026, 9);

        verify(transactionRepository).findMonthlyIncomeAndExpenses(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                eq(OffsetDateTime.parse("2026-09-01T00:00:00Z")),
                eq(OffsetDateTime.parse("2026-10-01T00:00:00Z"))
        );
    }

    @Test
    void getSpendingByCategory_shouldReturnCategorySpending() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        UUID foodCategoryId = UUID.randomUUID();
        UUID transportCategoryId = UUID.randomUUID();

        // PostgreSQL returns one row per expense category:
        // category ID, category name, and aggregated spending.
        when(transactionRepository.findSpendingByCategory(
                eq(userId),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of(
                new Object[] {
                        foodCategoryId,
                        "Food",
                        new BigDecimal("12000.00")
                },
                new Object[] {
                        transportCategoryId,
                        "Transport",
                        new BigDecimal("5000.00")
                }
        ));

        List<SpendingByCategoryResponse> result =
                analyticsService.getSpendingByCategory(userId, 2026, 9);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).categoryId())
                .isEqualTo(foodCategoryId);

        assertThat(result.get(0).categoryName())
                .isEqualTo("Food");

        assertThat(result.get(0).amount())
                .isEqualByComparingTo("12000.00");

        assertThat(result.get(1).categoryId())
                .isEqualTo(transportCategoryId);

        assertThat(result.get(1).categoryName())
                .isEqualTo("Transport");

        assertThat(result.get(1).amount())
                .isEqualByComparingTo("5000.00");
    }

    @Test
    void getSpendingByCategory_shouldReturnEmptyListWhenNoExpensesExist() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // No expense transactions means PostgreSQL returns no grouped rows.
        when(transactionRepository.findSpendingByCategory(
                eq(userId),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        List<SpendingByCategoryResponse> result =
                analyticsService.getSpendingByCategory(userId, 2026, 9);

        assertThat(result).isEmpty();
    }

    @Test
    void getSpendingByCategory_shouldRejectInvalidMonth() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                analyticsService.getSpendingByCategory(userId, 2026, 13)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid year or month");

        // Invalid input must be rejected before querying transaction data.
        verify(transactionRepository, never())
                .findSpendingByCategory(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getSpendingByCategory_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThatThrownBy(() ->
                analyticsService.getSpendingByCategory(userId, 2026, 9)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        // Analytics must never query transactions outside a valid user scope.
        verify(transactionRepository, never())
                .findSpendingByCategory(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getSpendingByCategory_shouldUseCorrectMonthlyDateRange() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(transactionRepository.findSpendingByCategory(
                eq(userId),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        analyticsService.getSpendingByCategory(userId, 2026, 9);

        verify(transactionRepository).findSpendingByCategory(
                eq(userId),
                eq(TransactionType.EXPENSE),
                eq(OffsetDateTime.parse("2026-09-01T00:00:00Z")),
                eq(OffsetDateTime.parse("2026-10-01T00:00:00Z"))
        );
    }

    @Test
    void getIncomeVsExpense_shouldReturnMonthlyIncomeAndExpenses() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // PostgreSQL returns one row for each month containing financial activity.
        when(transactionRepository.findIncomeVsExpenseByMonth(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of(
                new Object[] {
                        1,
                        new BigDecimal("75000.00"),
                        new BigDecimal("42000.00")
                },
                new Object[] {
                        2,
                        new BigDecimal("75000.00"),
                        new BigDecimal("38000.00")
                },
                new Object[] {
                        9,
                        new BigDecimal("80000.00"),
                        new BigDecimal("45000.00")
                }
        ));

        List<IncomeVsExpenseResponse> result =
                analyticsService.getIncomeVsExpense(userId, 2026);

        assertThat(result).hasSize(3);

        assertThat(result.get(0).month())
                .isEqualTo(1);

        assertThat(result.get(0).income())
                .isEqualByComparingTo("75000.00");

        assertThat(result.get(0).expenses())
                .isEqualByComparingTo("42000.00");

        assertThat(result.get(1).month())
                .isEqualTo(2);

        assertThat(result.get(1).income())
                .isEqualByComparingTo("75000.00");

        assertThat(result.get(1).expenses())
                .isEqualByComparingTo("38000.00");

        assertThat(result.get(2).month())
                .isEqualTo(9);

        assertThat(result.get(2).income())
                .isEqualByComparingTo("80000.00");

        assertThat(result.get(2).expenses())
                .isEqualByComparingTo("45000.00");
    }

    @Test
    void getIncomeVsExpense_shouldReturnEmptyListWhenNoTransactionsExist() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // No transactions means the grouped query returns no rows.
        when(transactionRepository.findIncomeVsExpenseByMonth(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        List<IncomeVsExpenseResponse> result =
                analyticsService.getIncomeVsExpense(userId, 2026);

        assertThat(result).isEmpty();
    }

    @Test
    void getIncomeVsExpense_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThatThrownBy(() ->
                analyticsService.getIncomeVsExpense(userId, 2026)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        // Analytics must never query transaction data outside a valid user scope.
        verify(transactionRepository, never())
                .findIncomeVsExpenseByMonth(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getIncomeVsExpense_shouldUseCorrectYearDateRange() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(transactionRepository.findIncomeVsExpenseByMonth(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of());

        analyticsService.getIncomeVsExpense(userId, 2026);

        verify(transactionRepository).findIncomeVsExpenseByMonth(
                eq(userId),
                eq(TransactionType.INCOME),
                eq(TransactionType.EXPENSE),
                eq(OffsetDateTime.parse("2026-01-01T00:00:00Z")),
                eq(OffsetDateTime.parse("2027-01-01T00:00:00Z"))
        );
    }

    @Test
    void getAccountBalances_shouldReturnBalancesForUserAccounts() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        Account savingsAccount =
                new Account(null, "HDFC Savings", AccountType.BANK, "INR");

        Account cashAccount =
                new Account(null, "Cash", AccountType.CASH, "INR");

        when(accountRepository.findByUser_Id(userId))
                .thenReturn(List.of(savingsAccount, cashAccount));

        when(accountBalanceService.calculateBalance(savingsAccount.getId()))
                .thenReturn(new BigDecimal("83000.00"));

        when(accountBalanceService.calculateBalance(cashAccount.getId()))
                .thenReturn(new BigDecimal("5000.00"));

        List<AccountBalanceResponse> result =
                analyticsService.getAccountBalances(userId);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).accountId())
                .isEqualTo(savingsAccount.getId());

        assertThat(result.get(0).accountName())
                .isEqualTo("HDFC Savings");

        assertThat(result.get(0).balance())
                .isEqualByComparingTo("83000.00");

        assertThat(result.get(1).accountId())
                .isEqualTo(cashAccount.getId());

        assertThat(result.get(1).accountName())
                .isEqualTo("Cash");

        assertThat(result.get(1).balance())
                .isEqualByComparingTo("5000.00");
    }

    @Test
    void getAccountBalances_shouldReturnEmptyListWhenUserHasNoAccounts() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        // A valid user with no accounts should simply return no balances.
        when(accountRepository.findByUser_Id(userId))
                .thenReturn(List.of());

        List<AccountBalanceResponse> result =
                analyticsService.getAccountBalances(userId);

        assertThat(result).isEmpty();

        verify(accountBalanceService, never())
                .calculateBalance(any());
    }

    @Test
    void getAccountBalances_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.existsById(userId))
                .thenReturn(false);

        assertThatThrownBy(() ->
                analyticsService.getAccountBalances(userId)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");

        // Invalid users must not be allowed to access account data.
        verify(accountRepository, never())
                .findByUser_Id(any());

        verify(accountBalanceService, never())
                .calculateBalance(any());
    }

    @Test
    void getAccountBalances_shouldDelegateBalanceCalculationToAccountBalanceService() {

        when(userRepository.existsById(userId))
                .thenReturn(true);

        Account savingsAccount =
                new Account(null, "HDFC Savings", AccountType.BANK, "INR");

        Account cashAccount =
                new Account(null, "Cash", AccountType.CASH, "INR");

        when(accountRepository.findByUser_Id(userId))
                .thenReturn(List.of(savingsAccount, cashAccount));

        when(accountBalanceService.calculateBalance(savingsAccount.getId()))
                .thenReturn(new BigDecimal("83000.00"));

        when(accountBalanceService.calculateBalance(cashAccount.getId()))
                .thenReturn(new BigDecimal("5000.00"));

        analyticsService.getAccountBalances(userId);

        // Analytics delegates the actual balance calculation instead of
        // duplicating transfer/credit-card/account balance rules.
        verify(accountBalanceService)
                .calculateBalance(savingsAccount.getId());

        verify(accountBalanceService)
                .calculateBalance(cashAccount.getId());

        verify(accountBalanceService, times(2))
                .calculateBalance(any());
    }

}
