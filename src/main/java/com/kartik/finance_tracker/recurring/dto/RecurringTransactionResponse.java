package com.kartik.finance_tracker.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.recurring.RecurringFrequencyUnit;
import com.kartik.finance_tracker.transactions.TransactionType;

public record RecurringTransactionResponse(
        UUID id,
        UUID accountId,
        UUID categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String description,
        RecurringFrequencyUnit frequencyUnit,
        int frequencyInterval,
        LocalDate nextOccurrence,
        LocalDate pausedUntil,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
