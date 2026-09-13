package com.kartik.finance_tracker.transactions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountType;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryType;
import com.kartik.finance_tracker.users.User;

public class TransactionControllerTest {

    @Test
    void createTransaction_shouldReturnCreatedExpense() throws Exception {
        TransactionService transactionService = mock(TransactionService.class);

        TransactionController transactionController =
                new TransactionController(transactionService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(transactionController)
                .build();

        UUID userId = UUID.randomUUID();

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Category category = new Category(
                user,
                "Food",
                CategoryType.EXPENSE,
                null,
                true
        );

        Transaction transaction = new Transaction(
                user,
                account,
                null,
                category,
                TransactionType.EXPENSE,
                new BigDecimal("850.00"),
                "Groceries",
                OffsetDateTime.parse("2026-09-13T10:00:00+05:30")
        );

        when(transactionService.createTransaction(
                any(UUID.class),
                any(UUID.class),
                any(),
                any(),
                any(TransactionType.class),
                any(BigDecimal.class),
                any(String.class),
                any(OffsetDateTime.class)
        )).thenReturn(transaction);

        // The API should return 201 when an expense is successfully created.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "categoryId": "%s",
                            "type": "EXPENSE",
                            "amount": 850.00,
                            "description": "Groceries",
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(account.getId(), category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
                .andExpect(jsonPath("$.destinationAccountId").doesNotExist())
                .andExpect(jsonPath("$.categoryId").value(category.getId().toString()))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.amount").value(850.00))
                .andExpect(jsonPath("$.description").value("Groceries"));
    }

    @Test
    void createTransaction_shouldReturnCreatedTransfer() throws Exception {
        TransactionService transactionService = mock(TransactionService.class);

        TransactionController transactionController =
                new TransactionController(transactionService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(transactionController)
                .build();

        UUID userId = UUID.randomUUID();

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account sourceAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Account destinationAccount = new Account(
                user,
                "SBI Savings",
                AccountType.BANK,
                "INR"
        );

        Transaction transaction = new Transaction(
                user,
                sourceAccount,
                destinationAccount,
                null,
                TransactionType.TRANSFER,
                new BigDecimal("5000.00"),
                "Move money",
                OffsetDateTime.parse("2026-09-13T10:00:00+05:30")
        );

        when(transactionService.createTransaction(
                any(UUID.class),
                any(UUID.class),
                any(UUID.class),
                any(),
                any(TransactionType.class),
                any(BigDecimal.class),
                any(String.class),
                any(OffsetDateTime.class)
        )).thenReturn(transaction);

        // Transfers should return both the source and destination account IDs.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "destinationAccountId": "%s",
                            "type": "TRANSFER",
                            "amount": 5000.00,
                            "description": "Move money",
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(
                                sourceAccount.getId(),
                                destinationAccount.getId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(sourceAccount.getId().toString()))
                .andExpect(jsonPath("$.destinationAccountId")
                        .value(destinationAccount.getId().toString()))
                .andExpect(jsonPath("$.categoryId").doesNotExist())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.description").value("Move money"));
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenAccountIsMissing() throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        // Every transaction must have a source account.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "EXPENSE",
                            "amount": 850.00,
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenTypeIsMissing() throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        // Every transaction must specify whether it is income, expense, or transfer.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "amount": 850.00,
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenAmountIsMissing() throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        // Every transaction must have an amount.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "type": "EXPENSE",
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenAmountHasTooManyDecimalPlaces()
            throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        // The API allows at most four decimal places for transaction amounts.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "type": "EXPENSE",
                            "amount": 100.12345,
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenOccurredAtIsMissing()
            throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        // Every transaction must have the date and time when it occurred.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "type": "EXPENSE",
                            "amount": 850.00
                        }
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }

    @Test
    void createTransaction_shouldReturnBadRequestWhenDescriptionIsTooLong()
            throws Exception {
        TransactionService transactionService = mock(TransactionService.class);
        TransactionController transactionController = new TransactionController(transactionService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        UUID userId = UUID.randomUUID();

        String longDescription = "a".repeat(256);

        // Transaction descriptions are limited to 255 characters.
        mockMvc.perform(post("/api/transactions")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "accountId": "%s",
                            "type": "EXPENSE",
                            "amount": 850.00,
                            "description": "%s",
                            "occurredAt": "2026-09-13T10:00:00+05:30"
                        }
                        """.formatted(UUID.randomUUID(), longDescription)))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(transactionService);
    }
}