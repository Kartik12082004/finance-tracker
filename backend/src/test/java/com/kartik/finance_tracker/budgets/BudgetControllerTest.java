package com.kartik.finance_tracker.budgets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

        BudgetResponse response = createBudgetResponse(budgetId);

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.createBudget(
                eq(userId),
                eq(2026),
                eq(10),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(budgetId.toString()))
        .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
        .andExpect(jsonPath("$.categoryName").value("Food"))
        .andExpect(jsonPath("$.amount").value(10000.00))
        .andExpect(jsonPath("$.spentAmount").value(6350.00))
        .andExpect(jsonPath("$.remainingAmount").value(3650.00))
        .andExpect(jsonPath("$.spendingPercentage").value(63.50))
        .andExpect(jsonPath("$.year").value(2026))
        .andExpect(jsonPath("$.month").value(10));

        verify(currentUserService)
                .getCurrentUserId();

        verify(budgetService).createBudget(
                eq(userId),
                eq(2026),
                eq(10),
                any()
        );
    }

    @Test
    void shouldGetBudgetsForMonth() throws Exception {

        BudgetResponse response =
                createBudgetResponse(UUID.randomUUID());

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.getBudgetsForMonth(
                userId,
                2026,
                10
        )).thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].categoryId")
                .value(categoryId.toString()))
        .andExpect(jsonPath("$[0].categoryName")
                .value("Food"))
        .andExpect(jsonPath("$[0].amount")
                .value(10000.00))
        .andExpect(jsonPath("$[0].spentAmount")
                .value(6350.00))
        .andExpect(jsonPath("$[0].remainingAmount")
                .value(3650.00))
        .andExpect(jsonPath("$[0].spendingPercentage")
                .value(63.50))
        .andExpect(jsonPath("$[0].year")
                .value(2026))
        .andExpect(jsonPath("$[0].month")
                .value(10));

        verify(currentUserService)
                .getCurrentUserId();

        verify(budgetService)
                .getBudgetsForMonth(userId, 2026, 10);
    }

    @Test
    void shouldReturnEmptyListWhenNoBudgetsExist() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.getBudgetsForMonth(
                userId,
                2026,
                10
        )).thenReturn(List.of());

        mockMvc.perform(
                get("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

        verify(budgetService)
                .getBudgetsForMonth(userId, 2026, 10);
    }

    @Test
    void shouldUpdateBudget() throws Exception {

        UUID budgetId = UUID.randomUUID();

        BudgetResponse response =
                createBudgetResponse(budgetId);

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        when(budgetService.updateBudget(
                eq(userId),
                eq(budgetId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "amount": 12000.00
                }
                """;

        mockMvc.perform(
                put("/api/budgets/{budgetId}", budgetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id")
                .value(budgetId.toString()))
        .andExpect(jsonPath("$.amount")
                .value(10000.00));

        verify(currentUserService)
                .getCurrentUserId();

        verify(budgetService).updateBudget(
                eq(userId),
                eq(budgetId),
                any()
        );
    }

    @Test
    void shouldDeleteBudget() throws Exception {

        UUID budgetId = UUID.randomUUID();

        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(
                delete("/api/budgets/{budgetId}", budgetId)
        )
        .andExpect(status().isNoContent());

        verify(currentUserService)
                .getCurrentUserId();

        verify(budgetService)
                .deleteBudget(userId, budgetId);
    }

    @Test
    void shouldRejectMissingCategory() throws Exception {

        String request = """
                {
                    "amount": 10000.00
                }
                """;

        mockMvc.perform(
                post("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {

        String request = """
                {
                    "categoryId": "%s"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
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
                    "amount": 0
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .param("year", "2026")
                        .param("month", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingYear() throws Exception {

        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .param("month", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingMonth() throws Exception {

        String request = """
                {
                    "categoryId": "%s",
                    "amount": 10000.00
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/budgets")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidYear() throws Exception {

        mockMvc.perform(
                get("/api/budgets")
                        .param("year", "abc")
                        .param("month", "10")
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidMonth() throws Exception {

        mockMvc.perform(
                get("/api/budgets")
                        .param("year", "2026")
                        .param("month", "abc")
        )
        .andExpect(status().isBadRequest());
    }

    private BudgetResponse createBudgetResponse(UUID budgetId) {

        return new BudgetResponse(
                budgetId,
                categoryId,
                "Food",
                new BigDecimal("10000.00"),
                new BigDecimal("6350.00"),
                new BigDecimal("3650.00"),
                new BigDecimal("63.50"),
                2026,
                10,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
