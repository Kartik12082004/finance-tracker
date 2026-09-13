package com.kartik.finance_tracker.accounts.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.accounts.AccountType;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        BigDecimal openingBalance,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}