package com.kartik.finance_tracker.analytics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountBalanceService;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.analytics.dto.AccountBalanceResponse;
import com.kartik.finance_tracker.analytics.dto.IncomeVsExpenseResponse;
import com.kartik.finance_tracker.analytics.dto.MonthlySummaryResponse;
import com.kartik.finance_tracker.analytics.dto.SpendingByCategoryResponse;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.UserRepository;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;

    public AnalyticsService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
    }

    @Cacheable(
            cacheNames = "monthlySummary",
            key = "#userId + ':' + #year + ':' + #month"
    )
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(
            UUID userId,
            int year,
            int month
    ) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(year, month);
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Invalid year or month");
        }

        OffsetDateTime start =
                yearMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        OffsetDateTime end =
                yearMonth.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        Object[] result = transactionRepository.findMonthlyIncomeAndExpenses(
                userId,
                TransactionType.INCOME,
                TransactionType.EXPENSE,
                start,
                end
        );

        /*
         * The repository aggregation returns one row containing:
         * [0] -> income
         * [1] -> expenses
         *
         * Hibernate exposes that row as a nested Object[].
         */
        Object[] row = result != null && result.length > 0
                ? (Object[]) result[0]
                : null;

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        if (row != null) {
            if (row[0] != null) {
                income = (BigDecimal) row[0];
            }

            if (row[1] != null) {
                expenses = (BigDecimal) row[1];
            }
        }

        BigDecimal netSavings = income.subtract(expenses);

        BigDecimal savingsRate = BigDecimal.ZERO;

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings
                    .multiply(BigDecimal.valueOf(100))
                    .divide(income, 2, java.math.RoundingMode.HALF_UP);
        }

        return new MonthlySummaryResponse(
                year,
                month,
                income,
                expenses,
                netSavings,
                savingsRate
        );
    }

    @Transactional(readOnly = true)
    public List<SpendingByCategoryResponse> getSpendingByCategory(
            UUID userId,
            int year,
            int month
    ) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(year, month);
        } catch (java.time.DateTimeException e) {
            throw new IllegalArgumentException("Invalid year or month");
        }

        OffsetDateTime start =
                yearMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        OffsetDateTime end =
                yearMonth.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Object[]> results =
                transactionRepository.findSpendingByCategory(
                        userId,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        return results.stream()
                .map(row -> new SpendingByCategoryResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncomeVsExpenseResponse> getIncomeVsExpense(
            UUID userId,
            int year
    ) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        OffsetDateTime start =
                YearMonth.of(year, 1)
                        .atDay(1)
                        .atStartOfDay()
                        .atOffset(ZoneOffset.UTC);

        OffsetDateTime end =
                YearMonth.of(year + 1, 1)
                        .atDay(1)
                        .atStartOfDay()
                        .atOffset(ZoneOffset.UTC);

        List<Object[]> results =
                transactionRepository.findIncomeVsExpenseByMonth(
                        userId,
                        TransactionType.INCOME,
                        TransactionType.EXPENSE,
                        start,
                        end
                );

        return results.stream()
                .map(row -> new IncomeVsExpenseResponse(
                        ((Number) row[0]).intValue(),
                        (BigDecimal) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> getAccountBalances(UUID userId) {

        // Analytics are always calculated within the authenticated user's scope.
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        List<Account> accounts =
                accountRepository.findByUser_Id(userId);

        /*
         * Reuse AccountBalanceService so balance rules remain in one place.
         * This includes transfers and credit-card balance semantics.
         */
        return accounts.stream()
                .map(account -> new AccountBalanceResponse(
                        account.getId(),
                        account.getName(),
                        accountBalanceService.calculateBalance(account.getId())
                ))
                .toList();
    }
}
