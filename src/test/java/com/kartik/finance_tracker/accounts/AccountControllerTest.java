package com.kartik.finance_tracker.accounts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kartik.finance_tracker.users.User;

public class AccountControllerTest {

    @Test
    void createAccount_shouldReturnCreatedAccount() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        Account account = new Account(
                new User(
                        "kartik@example.com",
                        "hashed-password",
                        "Kartik"
                ),
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        when(accountService.createAccount(
                any(UUID.class),
                any(String.class),
                any(AccountType.class),
                any(String.class)
        )).thenReturn(account);

        // The API should return 201 when an account is successfully created.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "HDFC Savings",
                            "type": "BANK",
                            "currency": "INR"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("HDFC Savings"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.openingBalance").value(0));
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenNameIsMissing() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        // The API must reject a request without an account name.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "BANK",
                            "currency": "INR"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenTypeIsMissing() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        // The API must reject a request without an account type.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "HDFC Savings",
                            "currency": "INR"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenCurrencyIsMissing() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        // The API must reject a request without a currency.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "HDFC Savings",
                            "type": "BANK"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenCurrencyLengthIsInvalid() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        // Currency must be exactly three characters, such as INR.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "HDFC Savings",
                            "type": "BANK",
                            "currency": "IN"
                        }
                        """))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenNameIsTooLong() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        String longName = "a".repeat(101);

        // Account names are limited to 100 characters.
        mockMvc.perform(post("/api/accounts")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "%s",
                            "type": "BANK",
                            "currency": "INR"
                        }
                        """.formatted(longName)))
                .andExpect(status().isBadRequest());

        // Invalid requests must not reach the service layer.
        verifyNoInteractions(accountService);
    }

    @Test
    void getAccounts_shouldReturnUserAccounts() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        User user = new User(
                "kartik@example.com",
                "hashed-password",
                "Kartik"
        );

        Account savingsAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        Account cashAccount = new Account(
                user,
                "Cash",
                AccountType.CASH,
                "INR"
        );

        when(accountService.getAccounts(userId))
                .thenReturn(List.of(savingsAccount, cashAccount));

        // The API should return all accounts belonging to the requested user.
        mockMvc.perform(get("/api/accounts")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("HDFC Savings"))
                .andExpect(jsonPath("$[0].type").value("BANK"))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[1].name").value("Cash"))
                .andExpect(jsonPath("$[1].type").value("CASH"))
                .andExpect(jsonPath("$[1].currency").value("INR"));
    }

    @Test
    void getAccounts_shouldReturnEmptyListWhenUserHasNoAccounts() throws Exception {
        AccountService accountService = mock(AccountService.class);

        AccountController accountController =
                new AccountController(accountService);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        UUID userId = UUID.randomUUID();

        when(accountService.getAccounts(userId))
                .thenReturn(List.of());

        // A user with no accounts should receive an empty array rather than null.
        mockMvc.perform(get("/api/accounts")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}