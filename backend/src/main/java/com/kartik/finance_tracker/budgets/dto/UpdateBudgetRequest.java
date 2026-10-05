package com.kartik.finance_tracker.budgets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.kartik.finance_tracker.budgets.BudgetPeriod;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateBudgetRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        @DecimalMax(value = "9999999999999999.99")
        BigDecimal amount,

        @NotNull
        BudgetPeriod period,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {
}
