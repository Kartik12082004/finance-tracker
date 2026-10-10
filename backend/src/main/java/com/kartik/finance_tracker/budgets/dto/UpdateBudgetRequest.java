package com.kartik.finance_tracker.budgets.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateBudgetRequest(

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount

) {
}
