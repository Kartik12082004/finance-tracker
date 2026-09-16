package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {

        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);

        accountService = new AccountService(
                accountRepository,
                userRepository
        );

        // Use one test user for the account creation and ownership scenarios.
        user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );
    }

    @Test
    void createAccount_shouldSaveAndReturnAccount() {

        UUID userId = user.getId();

        // Account creation requires an existing user.
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Mock the repository response to represent a successfully saved account.
        Account savedAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        when(accountRepository.save(any(Account.class)))
                .thenReturn(savedAccount);

        Account result = accountService.createAccount(
                userId,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        // The service should return the account produced by the repository.
        assertThat(result).isSameAs(savedAccount);

        // Verify that the service looked up the correct user before creating the account.
        verify(userRepository).findById(userId);

        ArgumentCaptor<Account> accountCaptor =
                ArgumentCaptor.forClass(Account.class);

        // Capture the account passed to the repository so we can verify its contents.
        verify(accountRepository).save(accountCaptor.capture());

        Account saved = accountCaptor.getValue();

        // The new account should belong to the requested user.
        assertThat(saved.getUser()).isSameAs(user);

        // Verify that the supplied account details are preserved.
        assertThat(saved.getName()).isEqualTo("HDFC Savings");
        assertThat(saved.getType()).isEqualTo(AccountType.BANK);
        assertThat(saved.getCurrency()).isEqualTo("INR");

        // New accounts start with a zero opening balance.
        assertThat(saved.getOpeningBalance()).isZero();

        // Entity construction should generate the ID and timestamps.
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void getAccount_shouldReturnAccountWhenItBelongsToUser() {

        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccount(
                userId,
                accountId
        );

        // The service should return the account when the account
        // belongs to the authenticated user.
        assertThat(result).isSameAs(account);

        // The repository lookup must be scoped by both IDs.
        verify(accountRepository)
                .findByIdAndUser_Id(accountId, userId);
    }

    @Test
    void getAccount_shouldRejectAccountOwnedByAnotherUser() {

        UUID userId = user.getId();
        UUID anotherUsersAccountId = UUID.randomUUID();

        // Returning an empty result represents an account that either
        // does not exist or does not belong to the authenticated user.
        when(accountRepository.findByIdAndUser_Id(
                anotherUsersAccountId,
                userId
        )).thenReturn(Optional.empty());

        // A user must not be able to access another user's account
        // by supplying that account's ID.
        assertThatThrownBy(() ->
                accountService.getAccount(
                        userId,
                        anotherUsersAccountId
                ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        // Verify that the ownership-scoped query was actually used.
        verify(accountRepository)
                .findByIdAndUser_Id(
                        anotherUsersAccountId,
                        userId
                );
    }
}
