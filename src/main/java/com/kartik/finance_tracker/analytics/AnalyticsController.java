package com.kartik.finance_tracker.analytics;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kartik.finance_tracker.analytics.dto.AccountBalanceResponse;
import com.kartik.finance_tracker.analytics.dto.IncomeVsExpenseResponse;
import com.kartik.finance_tracker.analytics.dto.MonthlySummaryResponse;
import com.kartik.finance_tracker.analytics.dto.SpendingByCategoryResponse;
import com.kartik.finance_tracker.security.CurrentUserService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CurrentUserService currentUserService;

    public AnalyticsController(
            AnalyticsService analyticsService,
            CurrentUserService currentUserService
    ) {
        this.analyticsService = analyticsService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public MonthlySummaryResponse getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return analyticsService.getMonthlySummary(
                userId,
                year,
                month
        );
    }

    @GetMapping("/spending-by-category")
    public List<SpendingByCategoryResponse> getSpendingByCategory(
            @RequestParam int year,
            @RequestParam int month
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return analyticsService.getSpendingByCategory(
                userId,
                year,
                month
        );
    }

    @GetMapping("/income-vs-expense")
    public List<IncomeVsExpenseResponse> getIncomeVsExpense(
            @RequestParam int year
    ) {
        UUID userId = currentUserService.getCurrentUserId();

        return analyticsService.getIncomeVsExpense(
                userId,
                year
        );
    }

    @GetMapping("/account-balances")
    public List<AccountBalanceResponse> getAccountBalances() {

        UUID userId = currentUserService.getCurrentUserId();

        return analyticsService.getAccountBalances(userId);
    }

}
