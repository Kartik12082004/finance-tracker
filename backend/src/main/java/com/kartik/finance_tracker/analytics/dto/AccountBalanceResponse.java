package com.kartik.finance_tracker.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.kartik.finance_tracker.accounts.AccountType;

public record AccountBalanceResponse(
        UUID accountId,
        String accountName,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        BigDecimal creditLimit,
        BigDecimal availableCredit
) {
}
