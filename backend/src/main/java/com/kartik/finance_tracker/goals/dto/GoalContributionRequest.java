package com.kartik.finance_tracker.goals.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record GoalContributionRequest(

        @NotNull(message = "Source account is required")
        UUID sourceAccountId,

        @NotNull(message = "Contribution amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Contribution must be greater than zero"
        )
        @Digits(
                integer = 15,
                fraction = 2,
                message = "Contribution must have at most 2 decimal places"
        )
        BigDecimal amount
) {
}
