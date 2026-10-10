
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
        recurringTransactionService =
                mock(RecurringTransactionService.class);
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
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        RecurringTransactionResponse response = createResponse(
                LocalDate.now().plusDays(2),
                false,
                true
        );

        when(recurringTransactionService.createRecurringTransaction(
                eq(userId), any()
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
                    "nextOccurrence": "%s"
                }
                """.formatted(
                accountId,
                categoryId,
                LocalDate.now().plusDays(2)
        );

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
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(649.00))
                .andExpect(jsonPath("$.frequencyUnit").value("MONTH"))
                .andExpect(jsonPath("$.frequencyInterval").value(1))
                .andExpect(jsonPath("$.skipNextOccurrence").value(false));

        verify(recurringTransactionService)
                .createRecurringTransaction(eq(userId), any());
    }

    @Test
    void shouldGetRecurringTransactions() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        RecurringTransactionResponse response = createResponse(
                LocalDate.now().plusDays(2),
                true,
                true
        );

        when(recurringTransactionService.getRecurringTransactions(userId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/recurring-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("Netflix"))
                .andExpect(jsonPath("$[0].amount").value(649.00))
                .andExpect(jsonPath("$[0].skipNextOccurrence").value(true));

        verify(recurringTransactionService)
                .getRecurringTransactions(userId);
    }

    @Test
    void shouldUpdateRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        RecurringTransactionResponse response = createResponse(
                LocalDate.now().plusDays(5),
                false,
                true
        );

        when(recurringTransactionService.updateRecurringTransaction(
                eq(userId), eq(recurringTransactionId), any()
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
                    "nextOccurrence": "%s"
                }
                """.formatted(
                accountId,
                categoryId,
                LocalDate.now().plusDays(5)
        );

        mockMvc.perform(
                put("/api/recurring-transactions/{id}", recurringTransactionId)
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
    void shouldSkipNextOccurrence() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        LocalDate nextOccurrence = LocalDate.now().plusDays(2);
        RecurringTransactionResponse response = createResponse(
                nextOccurrence,
                true,
                true
        );

        when(recurringTransactionService.skipNextOccurrence(
                userId, recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                patch(
                        "/api/recurring-transactions/{id}/skip-next",
                        recurringTransactionId
                )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipNextOccurrence").value(true))
                .andExpect(jsonPath("$.nextOccurrence")
                        .value(nextOccurrence.toString()))
                .andExpect(jsonPath("$.active").value(true));

        verify(recurringTransactionService)
                .skipNextOccurrence(userId, recurringTransactionId);
    }

    @Test
    void shouldUndoSkipNextOccurrence() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        LocalDate nextOccurrence = LocalDate.now().plusDays(2);
        RecurringTransactionResponse response = createResponse(
                nextOccurrence,
                false,
                true
        );

        when(recurringTransactionService.undoSkipNextOccurrence(
                userId, recurringTransactionId
        )).thenReturn(response);

        mockMvc.perform(
                patch(
                        "/api/recurring-transactions/{id}/undo-skip",
                        recurringTransactionId
                )
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipNextOccurrence").value(false))
                .andExpect(jsonPath("$.nextOccurrence")
                        .value(nextOccurrence.toString()));

        verify(recurringTransactionService)
                .undoSkipNextOccurrence(userId, recurringTransactionId);
    }

    @Test
    void shouldDeleteRecurringTransaction() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);

        mockMvc.perform(
                delete("/api/recurring-transactions/{id}",
                        recurringTransactionId)
        )
                .andExpect(status().isNoContent());

        verify(recurringTransactionService)
                .deleteRecurringTransaction(userId, recurringTransactionId);
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
                    "nextOccurrence": "%s"
                }
                """.formatted(categoryId, LocalDate.now().plusDays(2));

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
                    "nextOccurrence": "%s"
                }
                """.formatted(
                accountId,
                categoryId,
                LocalDate.now().plusDays(2)
        );

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
                    "nextOccurrence": "%s"
                }
                """.formatted(
                accountId,
                categoryId,
                LocalDate.now().plusDays(2)
        );

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
                    "nextOccurrence": "%s"
                }
                """.formatted(
                accountId,
                categoryId,
                LocalDate.now().plusDays(2)
        );

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

    private RecurringTransactionResponse createResponse(
            LocalDate nextOccurrence,
            boolean skipNextOccurrence,
            boolean active
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
                nextOccurrence,
                skipNextOccurrence,
                active,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
