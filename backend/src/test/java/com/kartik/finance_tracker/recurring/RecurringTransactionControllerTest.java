package com.kartik.finance_tracker.recurring;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.kartik.finance_tracker.recurring.dto.RecurringTransactionResponse;
import com.kartik.finance_tracker.security.CurrentUserService;
import com.kartik.finance_tracker.transactions.TransactionType;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionControllerTest {

    private RecurringTransactionService recurringTransactionService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    private UUID userId;
    private UUID recurringTransactionId;
    private UUID accountId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        recurringTransactionService = mock(
                RecurringTransactionService.class
        );
        currentUserService = mock(CurrentUserService.class);

        RecurringTransactionController controller =
                new RecurringTransactionController(
                        recurringTransactionService,
                        currentUserService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        userId = UUID.randomUUID();
        recurringTransactionId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    void shouldCreateRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createResponse();

        when(recurringTransactionService.createRecurringTransaction(
                eq(userId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 649.00,
                    "description": "Netflix",
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-10-01"
                }
                """.formatted(accountId, categoryId);

        // The authenticated user owns the recurring transaction.
        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id")
                .value(recurringTransactionId.toString()))
        .andExpect(jsonPath("$.accountId")
                .value(accountId.toString()))
        .andExpect(jsonPath("$.categoryId")
                .value(categoryId.toString()))
        .andExpect(jsonPath("$.categoryName")
                .value("Entertainment"))
        .andExpect(jsonPath("$.type")
                .value("EXPENSE"))
        .andExpect(jsonPath("$.amount")
                .value(649.00))
        .andExpect(jsonPath("$.frequencyUnit")
                .value("MONTH"))
        .andExpect(jsonPath("$.frequencyInterval")
                .value(1));

        verify(recurringTransactionService)
                .createRecurringTransaction(eq(userId), any());
    }

    @Test
    void shouldGetRecurringTransactions() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createResponse();

        when(recurringTransactionService.getRecurringTransactions(userId))
                .thenReturn(List.of(response));

        // The API should return only recurring rules belonging to the user.
        mockMvc.perform(
                get("/api/recurring-transactions")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].description")
                .value("Netflix"))
        .andExpect(jsonPath("$[0].amount")
                .value(649.00));

        verify(recurringTransactionService)
                .getRecurringTransactions(userId);
    }

    @Test
    void shouldUpdateRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createResponse();

        when(recurringTransactionService.updateRecurringTransaction(
                eq(userId),
                eq(recurringTransactionId),
                any()
        )).thenReturn(response);

        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 699.00,
                    "description": "Netflix Premium",
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-11-01"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(
                put("/api/recurring-transactions/{id}",
                        recurringTransactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isOk());

        verify(recurringTransactionService)
                .updateRecurringTransaction(
                        eq(userId),
                        eq(recurringTransactionId),
                        any()
                );
    }

    @Test
    void shouldPauseRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createResponseWithPause(
                        LocalDate.of(2026, 12, 1)
                );

        when(recurringTransactionService.pauseRecurringTransaction(
                userId,
                recurringTransactionId,
                LocalDate.of(2026, 12, 1)
        )).thenReturn(response);

        // Pausing should send the requested resume date to the service.
        mockMvc.perform(
                patch("/api/recurring-transactions/{id}/pause",
                        recurringTransactionId)
                        .param("pausedUntil", "2026-12-01")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pausedUntil")
                .value("2026-12-01"));

        verify(recurringTransactionService)
                .pauseRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        LocalDate.of(2026, 12, 1)
                );
    }

    @Test
    void shouldResumeRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createResponse();

        when(recurringTransactionService.resumeRecurringTransaction(
                userId,
                recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                patch("/api/recurring-transactions/{id}/resume",
                        recurringTransactionId)
        )
        .andExpect(status().isOk());

        verify(recurringTransactionService)
                .resumeRecurringTransaction(
                        userId,
                        recurringTransactionId
                );
    }

    @Test
    void shouldDeactivateRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        RecurringTransactionResponse response =
                createInactiveResponse();

        when(recurringTransactionService.deactivateRecurringTransaction(
                userId,
                recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                patch("/api/recurring-transactions/{id}/deactivate",
                        recurringTransactionId)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

        verify(recurringTransactionService)
                .deactivateRecurringTransaction(
                        userId,
                        recurringTransactionId
                );
    }

    @Test
    void shouldDeleteRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);

        mockMvc.perform(
                delete("/api/recurring-transactions/{id}",
                        recurringTransactionId)
        )
        .andExpect(status().isNoContent());

        verify(recurringTransactionService)
                .deleteRecurringTransaction(
                        userId,
                        recurringTransactionId
                );
    }

    @Test
    void shouldRejectMissingAccount() throws Exception {
        String request = """
                {
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 649.00,
                    "description": "Netflix",
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-10-01"
                }
                """.formatted(categoryId);

        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingAmount() throws Exception {
        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "description": "Netflix",
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-10-01"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonPositiveAmount() throws Exception {
        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 0,
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-10-01"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingFrequency() throws Exception {
        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 649.00,
                    "frequencyInterval": 1,
                    "nextOccurrence": "2026-10-01"
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingNextOccurrence() throws Exception {
        String request = """
                {
                    "accountId": "%s",
                    "categoryId": "%s",
                    "type": "EXPENSE",
                    "amount": 649.00,
                    "frequencyUnit": "MONTH",
                    "frequencyInterval": 1
                }
                """.formatted(accountId, categoryId);

        mockMvc.perform(
                post("/api/recurring-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    private RecurringTransactionResponse createResponse() {
        return new RecurringTransactionResponse(
                recurringTransactionId,
                accountId,
                categoryId,
                "Entertainment",
                TransactionType.EXPENSE,
                new BigDecimal("649.00"),
                "Netflix",
                RecurringFrequencyUnit.MONTH,
                1,
                LocalDate.of(2026, 10, 1),
                null,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private RecurringTransactionResponse createResponseWithPause(
            LocalDate pausedUntil
    ) {
        return new RecurringTransactionResponse(
                recurringTransactionId,
                accountId,
                categoryId,
                "Entertainment",
                TransactionType.EXPENSE,
                new BigDecimal("649.00"),
                "Netflix",
                RecurringFrequencyUnit.MONTH,
                1,
                pausedUntil,
                pausedUntil,
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private RecurringTransactionResponse createInactiveResponse() {
        return new RecurringTransactionResponse(
                recurringTransactionId,
                accountId,
                categoryId,
                "Entertainment",
                TransactionType.EXPENSE,
                new BigDecimal("649.00"),
                "Netflix",
                RecurringFrequencyUnit.MONTH,
                1,
                LocalDate.of(2026, 10, 1),
                null,
                false,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
