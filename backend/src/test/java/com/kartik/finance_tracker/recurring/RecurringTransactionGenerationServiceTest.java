package com.kartik.finance_tracker.recurring;

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

import static org.junit.jupiter.api.Assertions.*;

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
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        occurrenceDate
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

        /*
         * The generated transaction must represent the recurring
         * rule's actual occurrence date.
         */
        assertEquals(
                occurrenceDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime(),
                occurredAtCaptor.getValue()
        );

        verify(recurringTransactionRepository)
                .save(recurringTransaction);
    }

    @Test
    void shouldAdvanceMonthlyOccurrence() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

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
                        occurrenceDate
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(occurrenceDate);

        assertEquals(
                LocalDate.of(2026, 10, 22),
                recurringTransaction.getNextOccurrence()
        );
    }

    @Test
    void shouldPreserveMonthlyDayAnchorAfterShortMonth() {
        LocalDate januaryOccurrence = LocalDate.of(2026, 1, 31);

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
                        januaryOccurrence
                );

        /*
         * January 31 -> February 28 because February has no 31st.
         */
        when(recurringTransactionRepository.findDueRecurringTransactions(
                januaryOccurrence
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(januaryOccurrence);

        assertEquals(
                LocalDate.of(2026, 2, 28),
                recurringTransaction.getNextOccurrence()
        );

        /*
         * The original day-of-month anchor must still be 31.
         * Therefore the following occurrence must return to March 31
         * rather than permanently drifting to March 28.
         */
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
    }

    @Test
    void shouldAdvanceByConfiguredFrequency() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction recurringTransaction =
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.INCOME,
                        new BigDecimal("30000.00"),
                        "Salary",
                        RecurringFrequencyUnit.MONTH,
                        3,
                        occurrenceDate
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(recurringTransaction));

        generationService.generateDueTransactions(occurrenceDate);

        /*
         * MONTH × 3 means the next occurrence is three months later.
         */
        assertEquals(
                LocalDate.of(2026, 12, 22),
                recurringTransaction.getNextOccurrence()
        );
    }

    @Test
    void shouldProcessMultipleDueRecurringTransactions() {
        LocalDate occurrenceDate = LocalDate.of(2026, 9, 22);

        RecurringTransaction netflix =
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        occurrenceDate
                );

        RecurringTransaction salary =
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.INCOME,
                        new BigDecimal("30000.00"),
                        "Salary",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        occurrenceDate
                );

        when(recurringTransactionRepository.findDueRecurringTransactions(
                occurrenceDate
        )).thenReturn(List.of(netflix, salary));

        generationService.generateDueTransactions(occurrenceDate);

        /*
         * Every due recurring rule should generate its own normal
         * transaction.
         */
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
                new RecurringTransaction(
                        user,
                        account,
                        category,
                        TransactionType.EXPENSE,
                        new BigDecimal("649.00"),
                        "Netflix",
                        RecurringFrequencyUnit.MONTH,
                        1,
                        occurrenceDate
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

        /*
         * The occurrence must remain unchanged when transaction
         * creation fails. Otherwise the failed transaction could
         * be permanently skipped.
         */
        assertEquals(
                occurrenceDate,
                recurringTransaction.getNextOccurrence()
        );

        verify(recurringTransactionRepository, never())
                .save(recurringTransaction);
    }
}
