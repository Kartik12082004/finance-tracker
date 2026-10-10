package com.kartik.finance_tracker.budgets.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateBudgetRequest(

        @NotNull
        UUID categoryId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount

) {
}
