package com.kartik.finance_tracker.recurring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.kartik.finance_tracker.recurring.RecurringFrequencyUnit;
import com.kartik.finance_tracker.transactions.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRecurringTransactionRequest(

        @NotNull(message = "Account is required")
        UUID accountId,

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotNull(message = "Type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Frequency unit is required")
        RecurringFrequencyUnit frequencyUnit,

        @NotNull(message = "Frequency interval is required")
        @Min(value = 1, message = "Frequency interval must be at least 1")
        Integer frequencyInterval,

        @NotNull(message = "Next occurrence is required")
        LocalDate nextOccurrence
) {}
