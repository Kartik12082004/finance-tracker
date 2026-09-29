package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.investments.InvestmentType;

public record InvestmentResponse(
        UUID id,
        String name,
        InvestmentType type,
        BigDecimal quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentValue,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
