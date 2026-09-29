package com.kartik.finance_tracker.recurring;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kartik.finance_tracker.transactions.TransactionService;

@Service
public class RecurringTransactionGenerationService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionService transactionService;

    public RecurringTransactionGenerationService(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionService transactionService
    ) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionService = transactionService;
    }

    /*
     * Generates normal transactions for all recurring rules
     * that are due on or before the supplied date.
     */
    @Transactional
    public void generateDueTransactions(LocalDate date) {
        List<RecurringTransaction> recurringTransactions =
                recurringTransactionRepository.findDueRecurringTransactions(date);

        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            generateTransaction(recurringTransaction);
        }
    }

    /*
     * A generated transaction is a normal Transaction.
     * We reuse TransactionService so all existing transaction
     * validation and balance rules remain in one place.
     */
    private void generateTransaction(
            RecurringTransaction recurringTransaction
    ) {
        LocalDate occurrenceDate =
                recurringTransaction.getNextOccurrence();

        transactionService.createTransaction(
                recurringTransaction.getUser().getId(),
                recurringTransaction.getAccount().getId(),
                null,
                recurringTransaction.getCategory().getId(),
                recurringTransaction.getType(),
                recurringTransaction.getAmount(),
                recurringTransaction.getDescription(),
                occurrenceDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime()
        );

        /*
         * Once the transaction is successfully created,
         * move the recurring rule to its next occurrence.
         */
        recurringTransaction.advanceNextOccurrence();

        recurringTransactionRepository.save(recurringTransaction);
    }
}
