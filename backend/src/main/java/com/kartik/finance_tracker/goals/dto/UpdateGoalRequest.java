package com.kartik.finance_tracker.goals.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGoalRequest(

        @NotBlank(message = "Goal name is required")
        @Size(max = 255, message = "Goal name must not exceed 255 characters")
        String name,

        @NotNull(message = "Target amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Target amount must be greater than zero"
        )
        BigDecimal targetAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate
) {}
