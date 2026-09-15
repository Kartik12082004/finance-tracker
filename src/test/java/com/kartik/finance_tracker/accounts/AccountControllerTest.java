package com.kartik.finance_tracker.accounts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kartik.finance_tracker.security.CurrentUserService;
import com.kartik.finance_tracker.users.User;

class AccountControllerTest {

    private AccountService accountService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;

    private UUID userId;

    @BeforeEach
    void setUp() {

        accountService = mock(AccountService.class);
        currentUserService = mock(CurrentUserService.class);

        AccountController accountController =
                new AccountController(
                        accountService,
                        currentUserService
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(accountController)
                .build();

        userId = UUID.randomUUID();

        // The controller gets the authenticated user from CurrentUserService.
        // Tests therefore do not need to construct or inject JWTs themselves.
        when(currentUserService.getCurrentUserId())
                .thenReturn(userId);
    }

    @Test
    void createAccount_shouldReturnCreatedAccount() throws Exception {

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

        // The controller must pass the authenticated user's ID to the service.
        verify(accountService).createAccount(
                userId,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );
    }

    @Test
    void createAccount_shouldReturnBadRequestWhenNameIsMissing() throws Exception {

        // The API must reject a request without an account name.
        mockMvc.perform(post("/api/accounts")
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

        // The API must reject a request without an account type.
        mockMvc.perform(post("/api/accounts")
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

        // The API must reject a request without a currency.
        mockMvc.perform(post("/api/accounts")
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

        // Currency must be exactly three characters, such as INR.
        mockMvc.perform(post("/api/accounts")
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

        String longName = "a".repeat(101);

        // Account names are limited to 100 characters.
        mockMvc.perform(post("/api/accounts")
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

        // The API should return all accounts belonging to the authenticated user.
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("HDFC Savings"))
                .andExpect(jsonPath("$[0].type").value("BANK"))
                .andExpect(jsonPath("$[0].currency").value("INR"))
                .andExpect(jsonPath("$[1].name").value("Cash"))
                .andExpect(jsonPath("$[1].type").value("CASH"))
                .andExpect(jsonPath("$[1].currency").value("INR"));

        // The service must receive the authenticated user's ID.
        verify(accountService).getAccounts(userId);
    }

    @Test
    void getAccounts_shouldReturnEmptyListWhenUserHasNoAccounts() throws Exception {

        when(accountService.getAccounts(userId))
                .thenReturn(List.of());

        // A user with no accounts should receive an empty array rather than null.
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // The authenticated user's ID must be used when querying accounts.
        verify(accountService).getAccounts(userId);
    }

    @Test
    void getAccount_shouldReturnAccountWhenItBelongsToUser() throws Exception {

        UUID accountId = UUID.randomUUID();

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

        when(accountService.getAccount(userId, accountId))
                .thenReturn(account);

        // The API should return the requested account when it belongs
        // to the authenticated user.
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("HDFC Savings"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.openingBalance").value(0));

        // The controller must pass both the authenticated user ID
        // and requested account ID to the service.
        verify(accountService).getAccount(
                userId,
                accountId
        );
    }
}
