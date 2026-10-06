package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateInvestmentRequest(

        @NotNull
        @DecimalMin("0.00")
        BigDecimal currentValue
) {
}
