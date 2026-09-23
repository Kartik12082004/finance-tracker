package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.investments.InvestmentTransactionType;

public record InvestmentTransactionResponse(
        UUID id,
        UUID investmentId,
        UUID accountId,
        InvestmentTransactionType type,
        BigDecimal amount,
        BigDecimal quantity,
        BigDecimal pricePerUnit,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {
}
