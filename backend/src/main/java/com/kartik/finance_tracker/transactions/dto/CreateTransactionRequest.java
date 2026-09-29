package com.kartik.finance_tracker.transactions.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.kartik.finance_tracker.transactions.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTransactionRequest(

        @NotNull(message = "Account is required")
        UUID accountId,

        UUID destinationAccountId,

        UUID categoryId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Transaction amount is required")
        @DecimalMin(value = "0.0001", message = "Transaction amount must be greater than zero")
        @Digits(integer = 15, fraction = 4, message = "Transaction amount must have at most 4 decimal places")
        BigDecimal amount,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "Transaction date is required")
        OffsetDateTime occurredAt
) {
}