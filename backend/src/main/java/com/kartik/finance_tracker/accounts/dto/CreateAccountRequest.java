package com.kartik.finance_tracker.accounts.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.kartik.finance_tracker.accounts.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank(message = "Account name is required")
        @Size(max = 100, message = "Account name must not exceed 100 characters")
        String name,

        @NotNull(message = "Account type is required")
        AccountType type,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @NotNull(message = "Opening balance is required")
        @DecimalMin(value = "0.0", message = "Opening balance cannot be negative")
        BigDecimal openingBalance,

        @DecimalMin(value = "0.01", message = "Credit limit must be greater than zero")
        BigDecimal creditLimit,

        UUID parentAccountId
) {}
