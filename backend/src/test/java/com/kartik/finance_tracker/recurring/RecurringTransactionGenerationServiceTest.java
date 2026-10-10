
package com.kartik.finance_tracker.recurring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.kartik.finance_tracker.accounts.Account;
import com.kartik.finance_tracker.categories.Category;
import com.kartik.finance_tracker.transactions.TransactionService;
import com.kartik.finance_tracker.transactions.TransactionType;
import com.kartik.finance_tracker.users.User;

class RecurringTransactionGenerationServiceTest {

    private RecurringTransactionRepository recurringTransactionRepository;
    private TransactionService transactionService;
    private RecurringTransactionGenerationService generationService;

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {
        recurringTransactionRepository =
                mock(RecurringTransactionRepository.class);
        transactionService = mock(TransactionService.class);

        generationService = new RecurringTransactionGenerationService(
                recurringTransactionRepository,
                transactionService
        );

        user = mock(User.class);
        account = mock(Account.class);
        category = mock(Category.class);
    }

    @Test
    void shouldGenerateTransactionForDueRecurringTransaction() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(occurrenceDate);

        ArgumentCaptor<OffsetDateTime> occurredAtCaptor =
                ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(transactionService).createTransaction(
                any(),
                any(),
                isNull(),
                any(),
                eq(TransactionType.EXPENSE),
                eq(new BigDecimal("649.00")),
                eq("Netflix"),
                occurredAtCaptor.capture()
        );

        assertEquals(
                occurrenceDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
                occurredAtCaptor.getValue()
        );

        assertEquals(
                LocalDate.of(2026, 10, 22),
                recurringTransaction.getNextOccurrence()
        );
        assertFalse(recurringTransaction.isSkipNextOccurrence());

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldSkipMarkedOccurrenceWithoutCreatingTransaction() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        recurringTransaction.skipNextOccurrence();

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(occurrenceDate);

        verify(transactionService, never()).createTransaction(
                any(),
                any(),
                isNull(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        assertEquals(
                LocalDate.of(2026, 10, 22),
                recurringTransaction.getNextOccurrence()
        );
        assertFalse(recurringTransaction.isSkipNextOccurrence());

        verify(recurringTransactionRepository).save(recurringTransaction);
    }

    @Test
    void shouldGenerateFollowingOccurrenceNormallyAfterSkippingOne() {
        LocalDate firstOccurrence = LocalDate.of(2026, 9, 22);
        LocalDate secondOccurrence = LocalDate.of(2026, 10, 22);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        firstOccurrence,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        recurringTransaction.skipNextOccurrence();

        when(recurringTransactionRepository.findDueRecurringTransactions(
                firstOccurrence
        )).thenReturn(List.of(recurringTransaction));

        // The first occurrence is skipped.
        generationService.generateDueTransactions(firstOccurrence);

        verify(transactionService, never()).createTransaction(
                any(),
                any(),
                isNull(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        assertEquals(
                secondOccurrence,
                recurringTransaction.getNextOccurrence()
        );
        assertFalse(recurringTransaction.isSkipNextOccurrence());

        // The following occurrence should generate normally.
        when(recurringTransactionRepository.findDueRecurringTransactions(
                secondOccurrence
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(secondOccurrence);

        verify(transactionService).createTransaction(
                any(),
                any(),
                isNull(),
                any(),
                eq(TransactionType.EXPENSE),
                eq(new BigDecimal("649.00")),
                eq("Netflix"),
                eq(secondOccurrence.atStartOfDay(ZoneOffset.UTC)
                        .toOffsetDateTime())
        );

        assertEquals(
                LocalDate.of(2026, 11, 22),
                recurringTransaction.getNextOccurrence()
        );
        assertFalse(recurringTransaction.isSkipNextOccurrence());

        verify(recurringTransactionRepository, times(2))
                .save(recurringTransaction);
    }

    @Test
    void shouldPreserveMonthlyDayAnchorAfterShortMonth() {
        LocalDate januaryOccurrence = LocalDate.of(2026, 1, 31);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        januaryOccurrence,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                januaryOccurrence
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(januaryOccurrence);

        assertEquals(
                LocalDate.of(2026, 2, 28),
                recurringTransaction.getNextOccurrence()
        );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                LocalDate.of(2026, 2, 28)
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(
                LocalDate.of(2026, 2, 28)
        );

        assertEquals(
                LocalDate.of(2026, 3, 31),
                recurringTransaction.getNextOccurrence()
        );
        assertEquals(31, recurringTransaction.getAnchorDay());
    }

    @Test
    void shouldAdvanceByConfiguredFrequency() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        3,
                        TransactionType.INCOME,
                        new BigDecimal("30000.00"),
                        "Salary"
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(occurrenceDate);

        assertEquals(
                LocalDate.of(2026, 12, 22),
                recurringTransaction.getNextOccurrence()
        );
    }

    @Test
    void shouldProcessMultipleDueRecurringTransactions() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction netflix =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        RecurringTransaction salary =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.INCOME,
                        new BigDecimal("30000.00"),
                        "Salary"
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(netflix, salary));

        generationService.generateDueTransactions(occurrenceDate);

        verify(transactionService, times(2)).createTransaction(
                any(),
                any(),
                isNull(),
                any(),
                any(),
                any(),
                any(),
                any()
        );

        verify(recurringTransactionRepository).save(netflix);
        verify(recurringTransactionRepository).save(salary);
    }

    @Test
    void shouldNotAdvanceOccurrenceWhenTransactionGenerationFails() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction recurringTransaction =
                createRecurringTransaction(
                        occurrenceDate,
                        RecurringFrequencyUnit.MONTH,
                        1,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix"
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        doThrow(new IllegalArgumentException("Insufficient funds"))
                .when(transactionService)
                .createTransaction(
                        any(),
                        any(),
                        isNull(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> generationService.generateDueTransactions(occurrenceDate)
        );

        assertEquals(
                occurrenceDate,
                recurringTransaction.getNextOccurrence()
        );

        verify(recurringTransactionRepository, never())
                .save(recurringTransaction);
    }

    private RecurringTransaction createRecurringTransaction(
            LocalDate occurrenceDate,
            RecurringFrequencyUnit frequencyUnit,
            int frequencyInterval,
            TransactionType type,
            BigDecimal amount,
            String description
    ) {
        return new RecurringTransaction(
                user,
                account,
                category,
                type,
                amount,
                description,
                frequencyUnit,
                frequencyInterval,
                occurrenceDate
        );
    }
}
