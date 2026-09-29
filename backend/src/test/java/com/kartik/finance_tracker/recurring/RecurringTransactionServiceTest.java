package com.kartik.finance_tracker.recurring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.accounts.AccountRepository;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.categories.CategoryRepository;
import com.kartik.finance_tracker.common.exception.ResourceNotFoundException;
import com.kartik.finance_tracker.recurring.dto.CreateRecurringTransactionRequest;
import com.kartik.finance_tracker.recurring.dto.RecurringTransactionResponse;
import com.kartik.finance_tracker.recurring.dto.UpdateRecurringTransactionRequest;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;
import com.kartik.finance_tracker.users.UserRepository;

class RecurringTransactionServiceTest {

    private RecurringTransactionRepository recurringTransactionRepository;
    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;

    private RecurringTransactionService recurringTransactionService;

    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private UUID recurringTransactionId;

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        recurringTransactionRepository = mock(
                RecurringTransactionRepository.class
        );
        userRepository = mock(UserRepository.class);
        accountRepository = mock(AccountRepository.class);
        categoryRepository = mock(CategoryRepository.class);

        recurringTransactionService = new RecurringTransactionService(
                recurringTransactionRepository,
                userRepository,
                accountRepository,
                categoryRepository
        );

        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        recurringTransactionId = UUID.randomUUID();

        user = mock(User.class);
        account = mock(Account.class);
        category = mock(Category.class);

        when(user.getId()).thenReturn(userId);
        when(account.getId()).thenReturn(accountId);
        when(account.getUser()).thenReturn(user);
        when(category.getId()).thenReturn(categoryId);
        when(category.getUser()).thenReturn(user);
        when(category.getName()).thenReturn("Food");
    }

    @Test
    void shouldCreateRecurringTransaction() {
        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(recurringTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.createRecurringTransaction(
                        userId,
                        request
                );

        assertNotNull(response);
        assertEquals(accountId, response.accountId());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Food", response.categoryName());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(new BigDecimal("649.00"), response.amount());
        assertEquals("Netflix", response.description());
        assertEquals(RecurringFrequencyUnit.MONTH, response.frequencyUnit());
        assertEquals(1, response.frequencyInterval());
        assertEquals(
                LocalDate.of(2026, 10, 1),
                response.nextOccurrence()
        );
        assertTrue(response.active());
        assertTrue(response.pausedUntil() == null);

        verify(recurringTransactionRepository).save(any());
    }

    @Test
    void shouldRejectMissingUser() {
        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> recurringTransactionService
                        .createRecurringTransaction(userId, request)
        );

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void shouldRejectAccountOwnedByAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = mock(User.class);

        when(otherUser.getId()).thenReturn(otherUserId);
        when(account.getUser()).thenReturn(otherUser);

        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService
                        .createRecurringTransaction(userId, request)
        );

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void shouldRejectCategoryOwnedByAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = mock(User.class);

        when(otherUser.getId()).thenReturn(otherUserId);
        when(category.getUser()).thenReturn(otherUser);

        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService
                        .createRecurringTransaction(userId, request)
        );

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void shouldRejectTransferType() {
        CreateRecurringTransactionRequest request =
                new CreateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.TRANSFER,
                        new BigDecimal("1000.00"),
                        "Transfer",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService
                        .createRecurringTransaction(userId, request)
        );

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void shouldGetRecurringTransactionsForUser() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(recurringTransactionRepository.findByUser_Id(userId))
                .thenReturn(List.of(recurringTransaction));

        List<RecurringTransactionResponse> responses =
                recurringTransactionService
                        .getRecurringTransactions(userId);

        assertEquals(1, responses.size());
        assertEquals(
                "Netflix",
                responses.get(0).description()
        );
        assertEquals(
                categoryId,
                responses.get(0).categoryId()
        );
    }

    @Test
    void shouldUpdateRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        UpdateRecurringTransactionRequest request =
                new UpdateRecurringTransactionRequest(
                        accountId,
                        categoryId,
                        TransactionType.EXPENSE,
                        new BigDecimal("699.00"),
                        "Netflix Premium",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 11, 1)
                );

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        when(accountRepository.findById(accountId))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));

        when(recurringTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.updateRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        request
                );

        assertEquals(new BigDecimal("699.00"), response.amount());
        assertEquals("Netflix Premium", response.description());
        assertEquals(
                LocalDate.of(2026, 11, 1),
                response.nextOccurrence()
        );

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldPauseRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        LocalDate pausedUntil = LocalDate.now().plusMonths(2);

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.pauseRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        pausedUntil
                );

        assertEquals(pausedUntil, response.pausedUntil());
        assertEquals(pausedUntil, response.nextOccurrence());
        assertTrue(response.active());

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldRejectPauseDateInPast() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService.pauseRecurringTransaction(
                        userId,
                        recurringTransactionId,
                        LocalDate.now().minusDays(1)
                )
        );

        verify(recurringTransactionRepository, never()).save(any());
    }

    @Test
    void shouldResumeRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        LocalDate pausedUntil = LocalDate.now().plusMonths(2);
        recurringTransaction.pause(pausedUntil);

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService.resumeRecurringTransaction(
                        userId,
                        recurringTransactionId
                );

        assertTrue(response.active());
        assertEquals(null, response.pausedUntil());

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldDeactivateRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        when(recurringTransactionRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecurringTransactionResponse response =
                recurringTransactionService
                        .deactivateRecurringTransaction(
                                userId,
                                recurringTransactionId
                        );

        assertFalse(response.active());
        assertEquals(null, response.pausedUntil());

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldDeleteRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                createRecurringTransaction();

        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.of(recurringTransaction));

        recurringTransactionService.deleteRecurringTransaction(
                userId,
                recurringTransactionId
        );

        verify(recurringTransactionRepository)
                .delete(recurringTransaction);
    }

    @Test
    void shouldRejectAccessToAnotherUsersRecurringTransaction() {
        when(recurringTransactionRepository
                .findByIdAndUser_Id(recurringTransactionId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> recurringTransactionService
                        .deleteRecurringTransaction(
                                userId,
                                recurringTransactionId
                        )
        );

        verify(recurringTransactionRepository, never())
                .delete(any());
    }

    private RecurringTransaction createRecurringTransaction() {
        RecurringTransaction recurringTransaction =
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        LocalDate.of(2026, 10, 1)
                );

        try {
            var idField = RecurringTransaction.class
                    .getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(recurringTransaction, recurringTransactionId);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not assign test recurring transaction ID",
                    exception
            );
        }

        return recurringTransaction;
    }
}
