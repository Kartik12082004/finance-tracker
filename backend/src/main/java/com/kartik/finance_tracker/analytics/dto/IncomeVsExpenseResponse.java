package com.kartik.finance_tracker.analytics.dto;

import java.math.BigDecimal;

public record IncomeVsExpenseResponse(
        int month,
        BigDecimal income,
        BigDecimal expenses
) {
}
