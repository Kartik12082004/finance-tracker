package com.kartik.finance_tracker.recurring;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionGenerationService generationService;

    public RecurringTransactionScheduler(
            RecurringTransactionGenerationService generationService
    ) {
        this.generationService = generationService;
    }

    /*
     * Check once every day for recurring transactions that are due.
     *
     * The scheduler only triggers the generation service.
     * Business logic stays inside the service layer.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDueTransactions() {
        generationService.generateDueTransactions(LocalDate.now());
    }
}
