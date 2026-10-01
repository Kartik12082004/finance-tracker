package com.kartik.finance_tracker.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
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

        accountService = new AccountService(
                accountRepository,
                userRepository,
                transactionRepository,
                investmentTransactionRepository
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
        BigDecimal openingBalance = new BigDecimal("25000.00");

        // Account creation requires an existing user.
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Mock the repository response to represent a successfully saved account.
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
                openingBalance
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

        // The opening balance supplied by the user must be preserved.
        assertThat(saved.getOpeningBalance())
                .isEqualByComparingTo("25000.00");

        // Entity construction should generate the ID and timestamps.
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void createAccount_shouldRejectOpeningBalanceWithMoreThanTwoDecimalPlaces() {

        UUID userId = user.getId();
        BigDecimal openingBalance = new BigDecimal("25000.123");

        assertThatThrownBy(() ->
                accountService.createAccount(
                        userId,
                        "HDFC Savings",
                        AccountType.BANK,
                        "INR",
                        openingBalance
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount cannot have more than 2 decimal places");

        // Invalid financial values must never reach the database.
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

        // The account is safe to delete only when no normal transaction references it.
        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(false);

        // Investment transactions also count as financial history.
        when(investmentTransactionRepository.existsByAccount_Id(accountId))
                .thenReturn(false);

        accountService.deleteAccount(userId, accountId);

        // Ownership must be verified before any deletion is attempted.
        verify(accountRepository)
                .findByIdAndUser_Id(accountId, userId);

        // Both sources of financial history must be checked.
        verify(transactionRepository)
                .existsByAccount_IdOrDestinationAccount_Id(
                        accountId,
                        accountId
                );

        verify(investmentTransactionRepository)
                .existsByAccount_Id(accountId);

        // Only an account with no financial history should be deleted.
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

        // The account is referenced by at least one normal transaction.
        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(true);

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Account cannot be deleted because it has financial transactions"
                );

        // Financial history must never be deleted implicitly.
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

        // No normal transactions, but investment history exists.
        when(transactionRepository.existsByAccount_IdOrDestinationAccount_Id(
                accountId,
                accountId
        )).thenReturn(false);

        when(investmentTransactionRepository.existsByAccount_Id(accountId))
                .thenReturn(true);

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Account cannot be deleted because it has financial transactions"
                );

        // Investment history must also prevent account deletion.
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void deleteAccount_shouldThrowWhenAccountDoesNotBelongToUser() {

        UUID userId = user.getId();
        UUID accountId = UUID.randomUUID();

        when(accountRepository.findByIdAndUser_Id(accountId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                accountService.deleteAccount(userId, accountId)
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Account not found");

        // Nothing should be deleted when the ownership check fails.
        verify(accountRepository).findByIdAndUser_Id(accountId, userId);
        verify(accountRepository, never()).delete(any(Account.class));
    }
}
