package com.kartik.finance_tracker.budgets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.kartik.finance_tracker.budgets.BudgetPeriod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateBudgetRequest(

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Period is required")
        BudgetPeriod period,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
