package com.kartik.finance_tracker.budgets.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal spendingPercentage,
        int year,
        int month,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
