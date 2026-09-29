package com.kartik.finance_tracker.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.analytics.dto.AccountBalanceResponse;
import com.kartik.finance_tracker.analytics.dto.IncomeVsExpenseResponse;
import com.kartik.finance_tracker.analytics.dto.MonthlySummaryResponse;
import com.kartik.finance_tracker.analytics.dto.SpendingByCategoryResponse;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.security.CurrentUserService;

class AnalyticsControllerTest {

    private AnalyticsService analyticsService;
    private CurrentUserService currentUserService;
    private AnalyticsController analyticsController;
    private MockMvc mockMvc;

    private UUID userId;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        currentUserService = mock(CurrentUserService.class);

        analyticsController =
                new AnalyticsController(
                        analyticsService,
                        currentUserService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(analyticsController)
                .build();

        userId = UUID.randomUUID();

        // The controller gets the authenticated user from CurrentUserService.
        // Tests therefore do not need to construct or inject JWTs themselves.
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);
    }

    @Test
    void getMonthlySummary_shouldReturnSummary() throws Exception {

        MonthlySummaryResponse response =
                new MonthlySummaryResponse(
                        2026,
                        9,
                        new BigDecimal("75000.00"),
                        new BigDecimal("42000.00"),
                        new BigDecimal("33000.00"),
                        new BigDecimal("44.00")
                );

        when(analyticsService.getMonthlySummary(userId, 2026, 9))
                .thenReturn(response);

        // A valid analytics request should return the calculated monthly summary.
        mockMvc.perform(get("/api/analytics/summary")
                .param("year", "2026")
                .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.totalIncome").value(75000.00))
                .andExpect(jsonPath("$.totalExpenses").value(42000.00))
                .andExpect(jsonPath("$.netSavings").value(33000.00))
                .andExpect(jsonPath("$.savingsRate").value(44.00));

        // Analytics must always be calculated for the authenticated user.
        verify(analyticsService)
                .getMonthlySummary(userId, 2026, 9);
    }

    @Test
    void getMonthlySummary_shouldReturnBadRequestWhenYearIsMissing()
            throws Exception {

        // The API requires both year and month to identify the requested period.
        mockMvc.perform(get("/api/analytics/summary")
                .param("month", "9"))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(analyticsService);
    }

    @Test
    void getMonthlySummary_shouldReturnBadRequestWhenMonthIsMissing()
            throws Exception {

        // The API requires both year and month to identify the requested period.
        mockMvc.perform(get("/api/analytics/summary")
                .param("year", "2026"))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(analyticsService);
    }

    @Test
    void getMonthlySummary_shouldReturnBadRequestWhenYearIsNotANumber()
            throws Exception {

        // Request parameters must be convertible to the controller's integer types.
        mockMvc.perform(get("/api/analytics/summary")
                .param("year", "abc")
                .param("month", "9"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void getMonthlySummary_shouldReturnBadRequestWhenMonthIsNotANumber()
            throws Exception {

        // Request parameters must be convertible to the controller's integer types.
        mockMvc.perform(get("/api/analytics/summary")
                .param("year", "2026")
                .param("month", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analyticsService);
    }

    @Test
    void getMonthlySummary_shouldPassRequestedYearAndMonthToService()
            throws Exception {

        MonthlySummaryResponse response =
                new MonthlySummaryResponse(
                        2025,
                        12,
                        new BigDecimal("50000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("20000.00"),
                        new BigDecimal("40.00")
                );

        when(analyticsService.getMonthlySummary(userId, 2025, 12))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/summary")
                .param("year", "2025")
                .param("month", "12"))
                .andExpect(status().isOk());

        // The controller must pass the requested period unchanged to the service.
        verify(analyticsService)
                .getMonthlySummary(userId, 2025, 12);
    }

    @Test
    void getSpendingByCategory_shouldReturnSpendingData() throws Exception {

        UUID categoryId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getSpendingByCategory(
                userId,
                2026,
                9
        )).thenReturn(List.of(
                new SpendingByCategoryResponse(
                        categoryId,
                        "Food",
                        new BigDecimal("12000.00")
                )
        ));

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("year", "2026")
                .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryId")
                        .value(categoryId.toString()))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$[0].amount")
                        .value(12000.00));

        verify(currentUserService)
                .getCurrentUserId();

        verify(analyticsService)
                .getSpendingByCategory(userId, 2026, 9);
    }

    @Test
    void getSpendingByCategory_shouldReturnEmptyListWhenNoSpendingExists()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getSpendingByCategory(
                userId,
                2026,
                9
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("year", "2026")
                .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getSpendingByCategory_shouldRejectMissingYear()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("month", "9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSpendingByCategory_shouldRejectMissingMonth()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("year", "2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSpendingByCategory_shouldRejectInvalidYear()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("year", "abc")
                .param("month", "9"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSpendingByCategory_shouldRejectInvalidMonth()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(get("/api/analytics/spending-by-category")
                .param("year", "2026")
                .param("month", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIncomeVsExpense_shouldReturnMonthlyData() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getIncomeVsExpense(
                userId,
                2026
        )).thenReturn(List.of(
                new IncomeVsExpenseResponse(
                        1,
                        new BigDecimal("75000.00"),
                        new BigDecimal("42000.00")
                ),
                new IncomeVsExpenseResponse(
                        2,
                        new BigDecimal("75000.00"),
                        new BigDecimal("38000.00")
                )
        ));

        mockMvc.perform(get("/api/analytics/income-vs-expense")
                .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value(1))
                .andExpect(jsonPath("$[0].income").value(75000.00))
                .andExpect(jsonPath("$[0].expenses").value(42000.00))
                .andExpect(jsonPath("$[1].month").value(2))
                .andExpect(jsonPath("$[1].income").value(75000.00))
                .andExpect(jsonPath("$[1].expenses").value(38000.00));

        verify(currentUserService)
                .getCurrentUserId();

        verify(analyticsService)
                .getIncomeVsExpense(userId, 2026);
    }

    @Test
    void getIncomeVsExpense_shouldReturnEmptyListWhenNoDataExists()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getIncomeVsExpense(
                userId,
                2026
        )).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/income-vs-expense")
                .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getIncomeVsExpense_shouldRejectMissingYear()
            throws Exception {

        mockMvc.perform(get("/api/analytics/income-vs-expense"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(currentUserService, analyticsService);
    }

    @Test
    void getIncomeVsExpense_shouldRejectInvalidYear()
            throws Exception {

        mockMvc.perform(get("/api/analytics/income-vs-expense")
                .param("year", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(currentUserService, analyticsService);
    }

    @Test
    void getAccountBalances_shouldReturnBalances() throws Exception {

        UUID accountId = UUID.randomUUID();

        List<AccountBalanceResponse> response = List.of(
                new AccountBalanceResponse(
                        accountId,
                        "HDFC Savings",
                        new BigDecimal("83000.00")
                )
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getAccountBalances(userId))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/account-balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId")
                        .value(accountId.toString()))
                .andExpect(jsonPath("$[0].accountName")
                        .value("HDFC Savings"))
                .andExpect(jsonPath("$[0].balance")
                        .value(83000.00));

        // The controller must use the authenticated user's ID rather than accepting
        // an arbitrary user ID from the request.
        verify(analyticsService)
                .getAccountBalances(userId);
    }

    @Test
    void getAccountBalances_shouldReturnEmptyListWhenUserHasNoAccounts()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(analyticsService.getAccountBalances(userId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/account-balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // No accounts is a valid state, so the API returns an empty list rather
        // than treating it as an error.
        verify(analyticsService)
                .getAccountBalances(userId);
    }

    @Test
    void getAccountBalances_shouldPropagateUserNotFoundException() {

        when(analyticsService.getAccountBalances(userId))
                .thenThrow(new ResourceNotFoundException("User not found"));

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> analyticsController.getAccountBalances()
                );

        assertEquals("User not found", exception.getMessage());

        verify(analyticsService)
                .getAccountBalances(userId);
    }
}
