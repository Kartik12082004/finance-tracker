package com.kartik.finance_tracker.analytics.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpendingByCategoryResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal amount
) {
}
