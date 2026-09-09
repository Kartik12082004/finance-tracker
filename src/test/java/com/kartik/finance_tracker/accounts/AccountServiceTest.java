package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

public class AccountServiceTest {
    
    @Test 
    void createAccount_shouldSaveAndReturnAccount() {
        
        AccountRepository accountRepository = mock(AccountRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        AccountService accountService = new AccountService(accountRepository, userRepository);

        User user = new User(
                "kartik@example.com",
                "hashedpassword",
                "Kartik"
        );

        UUID userId = user.getId();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Account savedAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        Account result = accountService.createAccount(
                userId,
                "HDFC Savings",
                AccountType.BANK,
                "INR"
        );

        assertThat(result).isSameAs(savedAccount);

        verify(userRepository).findById(userId);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account accountPassedToRepository = accountCaptor.getValue();

        assertThat(accountPassedToRepository.getUser())
                .isSameAs(user);
        assertThat(accountPassedToRepository.getName())
                .isEqualTo("HDFC Savings");
        assertThat(accountPassedToRepository.getType())
                .isEqualTo(AccountType.BANK);
        assertThat(accountPassedToRepository.getCurrency())
                .isEqualTo("INR");
        assertThat(accountPassedToRepository.getId())
                .isNotNull();   
        assertThat(accountPassedToRepository.getOpeningBalance())
                .isZero();
        assertThat(accountPassedToRepository.getCreatedAt())
                .isNotNull();
        assertThat(accountPassedToRepository.getUpdatedAt())
                .isNotNull();
        
    }

}
