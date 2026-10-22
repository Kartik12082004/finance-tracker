
package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.goals.GoalRepository;
import com.kartik.finance_tracker.investments.InvestmentTransactionRepository;
import com.kartik.finance_tracker.transactions.TransactionRepository;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private UserRepository userRepository;
    private TransactionRepository transactionRepository;
    private InvestmentTransactionRepository investmentTransactionRepository;
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        userRepository = mock(UserRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        investmentTransactionRepository = mock(InvestmentTransactionRepository.class);
        GoalRepository goalRepository = mock(GoalRepository.class);

        accountService = new AccountService(
                accountRepository,
                userRepository,
                transactionRepository,
                investmentTransactionRepository,
                goalRepository
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
        BigDecimal openingBalance = new BigDecimal("25000.00");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Account savedAccount = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                openingBalance
        );

        when(accountRepository.save(any(Account.class)))
                .thenReturn(savedAccount);

        Account result = accountService.createAccount(
                userId,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                openingBalance,
                null,
                null
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
        assertThat(saved.getOpeningBalance())
                .isEqualByComparingTo("25000.00");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void createAccount_shouldRejectGoalAccountType() {
        assertThatThrownBy(() ->
                accountService.createAccount(
                        user.getId(),
                        "Goal - Gaming PC",
                        AccountType.GOAL,
                        "INR",
                        BigDecimal.ZERO,
                        null,
                        null
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Goal accounts can only be created through goal management"
                );

        verifyNoInteractions(userRepository);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccount_shouldReturnAccountWhenItBelongsToUser() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                new BigDecimal("25000.00")
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        Account result = accountService.getAccount(userId, accountId);

        assertThat(result).isSameAs(account);

        verify(accountRepository)
                .findByIdAndUser_Id(accountId, userId);
    }

    @Test
    void getAccount_shouldRejectAccountOwnedByAnotherUser() {
        UUID userId = user.getId();
        UUID anotherUsersAccountId = UUID.randomUUID();

        when(accountRepository.findByIdAndUser_Id(
                anotherUsersAccountId,
                userId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.getAccount(userId, anotherUsersAccountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        verify(accountRepository)
                .findByIdAndUser_Id(anotherUsersAccountId, userId);
    }

    @Test
    void updateAccount_shouldRejectUpdatingGoalAccount() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(goalAccount));

        assertThatThrownBy(() ->
                accountService.updateAccount(
                        userId,
                        accountId,
                        "Renamed Goal",
                        AccountType.GOAL,
                        "INR",
                        null,
                        null
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Goal accounts cannot be updated through account management"
                );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccount_shouldRejectConvertingNormalAccountToGoalAccount() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                new BigDecimal("25000.00")
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                accountService.updateAccount(
                        userId,
                        accountId,
                        "Goal - Gaming PC",
                        AccountType.GOAL,
                        "INR",
                        null,
                        null
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "An account cannot be converted into a goal account"
                );

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void deleteAccount_shouldDeleteAccountWhenItHasNoFinancialHistory() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                BigDecimal.ZERO
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(false);

        when(investmentTransactionRepository.existsByAccount_Id(accountId))
                .thenReturn(false);

        accountService.deleteAccount(userId, accountId);

        verify(accountRepository)
                .findByIdAndUser_Id(accountId, userId);

        verify(transactionRepository)
                .existsByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        verify(investmentTransactionRepository)
                .existsByAccount_Id(accountId);

        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccount_shouldRejectAccountWithTransactions() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "HDFC Savings",
                AccountType.BANK,
                "INR",
                BigDecimal.ZERO
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(true);

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Account cannot be deleted because it has financial transactions"
                );

        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void deleteAccount_shouldRejectAccountWithInvestmentTransactions() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account account = new Account(
                user,
                "Mutual Funds",
                AccountType.INVESTMENT,
                "INR",
                BigDecimal.ZERO
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(account));

        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(false);

        when(investmentTransactionRepository.existsByAccount_Id(accountId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Account cannot be deleted because it has financial transactions"
                );

        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void deleteAccount_shouldRejectGoalAccount() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        Account goalAccount = new Account(
                user,
                "Goal - Gaming PC",
                AccountType.GOAL,
                "INR",
                BigDecimal.ZERO
        );

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.of(goalAccount));

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Goal accounts cannot be deleted through account management"
                );

        verify(accountRepository, never()).delete(any(Account.class));
        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(investmentTransactionRepository);
    }

    @Test
    void deleteAccount_shouldThrowWhenAccountDoesNotBelongToUser() {
        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        verify(accountRepository)
                .findByIdAndUser_Id(accountId, userId);

        verify(accountRepository, never()).delete(any(Account.class));
    }
}
