package com.kartik.finance_tracker.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID accountId,
        String accountName,
        BigDecimal balance
) {
}
