package com.kartik.finance_tracker.budgets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.budgets.dto.BudgetResponse;
import com.kartik.finance_tracker.security.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private BudgetService budgetService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    private UUID userId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        budgetService = mock(BudgetService.class);
        currentUserService = mock(CurrentUserService.class);

        BudgetController budgetController = new BudgetController(
                budgetService,
                currentUserService
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(budgetController)
                .build();

        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    void shouldCreateBudget() throws Exception {
        UUID budgetId = UUID.randomUUID();

        BudgetResponse response = new BudgetResponse(
                budgetId,
                categoryId,
                "Food",
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.createBudget(
                eq(userId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00,
                    "period": "MONTHLY",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30"
                }
                """.formatted(categoryId);

        // The API should create a budget for the authenticated user.
        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(budgetId.toString()))
        .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
        .andExpect(jsonPath("$.categoryName").value("Food"))
        .andExpect(jsonPath("$.amount").value(10000.00))
        .andExpect(jsonPath("$.period").value("MONTHLY"));

        verify(budgetService).createBudget(eq(userId), any());
    }

    @Test
    void shouldGetAllBudgets() throws Exception {
        BudgetResponse response = createBudgetResponse();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.getBudgets(userId))
                .thenReturn(List.of(response));

        // The API should return all budgets belonging to the authenticated user.
        mockMvc.perform(
                get("/api/budgets")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].categoryName").value("Food"))
        .andExpect(jsonPath("$[0].amount").value(10000.00));

        verify(budgetService).getBudgets(userId);
    }

    @Test
    void shouldGetBudgetsForDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 15);

        BudgetResponse response = createBudgetResponse();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.getBudgetsForDate(userId, date))
                .thenReturn(List.of(response));

        // The API should return budgets active on the requested date.
        mockMvc.perform(
                get("/api/budgets")
                        .param("date", "2026-09-15")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].categoryName").value("Food"));

        verify(budgetService).getBudgetsForDate(userId, date);
    }

    @Test
    void shouldRejectMissingCategory() throws Exception {
        String request = """
                {
                    "amount": 10000.00,
                    "period": "MONTHLY",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30"
                }
                """;

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "period": "MONTHLY",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonPositiveAmount() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "amount": 0,
                    "period": "MONTHLY",
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingPeriod() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00,
                    "startDate": "2026-09-01",
                    "endDate": "2026-09-30"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingStartDate() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00,
                    "period": "MONTHLY",
                    "endDate": "2026-09-30"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingEndDate() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00,
                    "period": "MONTHLY",
                    "startDate": "2026-09-01"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    private BudgetResponse createBudgetResponse() {
        return new BudgetResponse(
                UUID.randomUUID(),
                categoryId,
                "Food",
                new BigDecimal("10000.00"),
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
