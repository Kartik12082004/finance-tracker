package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
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

public class AccountServiceTest {

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

        user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );
    }

    @Test
    void createAccount_shouldSaveAndReturnAccount() {

        UUID userId = user.getId();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

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

        assertThat(result).isSameAs(savedAccount);

        verify(userRepository).findById(userId);

        ArgumentCaptor<Account> accountCaptor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository).save(accountCaptor.capture());

        Account saved = accountCaptor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo("HDFC Savings");
        assertThat(saved.getType()).isEqualTo(AccountType.BANK);
        assertThat(saved.getCurrency()).isEqualTo("INR");
        assertThat(saved.getOpeningBalance()).isZero();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}