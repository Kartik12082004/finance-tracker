package com.kartik.finance_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.transactions.TransactionType;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        UUID destinationAccountId,
        UUID categoryId,
        TransactionType type,
        BigDecimal amount,
        String description,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}