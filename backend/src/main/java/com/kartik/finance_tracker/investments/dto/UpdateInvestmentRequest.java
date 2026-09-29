package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;

import com.kartik.finance_tracker.investments.InvestmentType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInvestmentRequest(

        @NotBlank(message = "Investment name is required")
        @Size(max = 255, message = "Investment name must not exceed 255 characters")
        String name,

        @NotNull(message = "Investment type is required")
        InvestmentType type,

        @NotNull(message = "Current value is required")
        @DecimalMin(value = "0.00", message = "Current value cannot be negative")
        BigDecimal currentValue
) {
}
