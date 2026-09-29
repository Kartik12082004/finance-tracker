package com.kartik.finance_tracker.investments.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BuyInvestmentRequest(

        @NotNull(message = "Account is required")
        UUID accountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @DecimalMin(value = "0.00000001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @DecimalMin(value = "0.0001", message = "Price per unit must be greater than zero")
        BigDecimal pricePerUnit,

        @NotNull(message = "Occurred at is required")
        OffsetDateTime occurredAt
) {
}
